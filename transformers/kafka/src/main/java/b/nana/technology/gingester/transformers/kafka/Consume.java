package b.nana.technology.gingester.transformers.kafka;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import net.jodah.typetools.TypeResolver;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.serialization.Deserializer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class Consume implements Transformer<Object, Object> {

    private static final int INFINITE_PATIENCE = -1;

    private final List<String> topics;
    private final Duration pollDuration;
    private final int patience;
    private final CommitMode commitMode;
    private final Class<?> keyDeserializer;
    private final Class<?> valueDeserializer;
    private final Properties properties;

    public Consume(Parameters parameters) {
        topics = parameters.topics;
        pollDuration = Duration.ofSeconds(parameters.pollSeconds);
        patience = parameters.patience;
        commitMode = parameters.commitMode;
        keyDeserializer = getDeserializer(parameters.keyDeserializer);
        valueDeserializer = getDeserializer(parameters.valueDeserializer);
        properties = new Properties();
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer.getName());
        parameters.properties.forEach(properties::setProperty);
    }

    @Override
    public Class<?> getOutputType() {
        return TypeResolver.resolveRawArguments(Deserializer.class, valueDeserializer)[0];
    }

    @Override
    public Map<String, Object> getStashDetails() {
        return Map.of(
                "key", TypeResolver.resolveRawArguments(Deserializer.class, keyDeserializer)[0]
        );
    }

    private Class<?> getDeserializer(String parameter) {
        try {
            String className = "org.apache.kafka.common.serialization." + parameter + "Deserializer";
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName(parameter);
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("No class found for " + parameter);
            }
        }
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {

        try (Consumer<?, ?> consumer = new KafkaConsumer<>(properties)) {

            consumer.subscribe(topics);

            int emptyPollStreak = 0;
            while (true) {

                ConsumerRecords<?, ?> records;

                try {
                    records = consumer.poll(pollDuration);
                } catch (InterruptException e) {
                    boolean interrupted = Thread.interrupted();  // clear interrupt status
                    if (!interrupted)
                        throw new IllegalStateException("InterruptException caught but no interrupt status");
                    break;
                }

                if (records.isEmpty()) {
                    if (patience == INFINITE_PATIENCE) {
                        continue;
                    } else {
                        emptyPollStreak++;
                        if (patience < emptyPollStreak) break;
                        else continue;
                    }
                }

                emptyPollStreak = 0;

                for (ConsumerRecord<?, ?> record : records) {
                    Object key = record.key();
                    if (key != null) {
                        out.accept(context.stash("key", key), record.value());
                    } else {
                        out.accept(context, record.value());
                    }
                }

                if (commitMode == CommitMode.PER_POLL) {
                    consumer.commitAsync();
                }
            }

            if (commitMode == CommitMode.PER_TRANSFORM) {
                consumer.commitAsync();
            }
        }
    }

    public static class Parameters {
        public List<String> topics;
        public int pollSeconds = 10;
        public int patience = INFINITE_PATIENCE;
        public CommitMode commitMode = CommitMode.PER_POLL;
        public String keyDeserializer = "ByteArray";
        public String valueDeserializer = "ByteArray";
        public Map<String, String> properties = Map.of();
    }

    public enum CommitMode {
        PER_POLL,
        PER_TRANSFORM
    }
}

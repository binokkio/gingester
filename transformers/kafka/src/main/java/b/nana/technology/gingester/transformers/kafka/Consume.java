package b.nana.technology.gingester.transformers.kafka;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class Consume implements Transformer<Object, byte[]> {

    private static final int INFINITE_PATIENCE = -1;

    private final List<String> topics;
    private final Duration pollDuration;
    private final int patience;
    private final CommitMode commitMode;
    private final Properties properties;

    public Consume(Parameters parameters) {
        topics = parameters.topics;
        pollDuration = Duration.ofSeconds(parameters.pollSeconds);
        patience = parameters.patience;
        commitMode = parameters.commitMode;
        properties = new Properties();
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        parameters.properties.forEach(properties::setProperty);
    }

    @Override
    public void transform(Context context, Object in, Receiver<byte[]> out) {

        try (Consumer<byte[], byte[]> consumer = new KafkaConsumer<>(properties)) {

            consumer.subscribe(topics);

            int emptyPollStreak = 0;
            while (true) {

                ConsumerRecords<byte[], byte[]> records;

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

                for (ConsumerRecord<byte[], byte[]> record : records) {
                    byte[] key = record.key();
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
        public Map<String, String> properties = Map.of();
    }

    public enum CommitMode {
        PER_POLL,
        PER_TRANSFORM
    }
}

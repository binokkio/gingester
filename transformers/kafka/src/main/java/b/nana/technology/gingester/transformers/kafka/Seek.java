package b.nana.technology.gingester.transformers.kafka;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

public final class Seek implements Transformer<Object, Void> {

    private final String topic;
    private final int partition;
    private final long seekTo;
    private final Properties properties;

    public Seek(Parameters parameters) {
        topic = requireNonNull(parameters.topic);
        partition = requireNonNull(parameters.partition);
        seekTo = requireNonNull(parameters.seekTo);
        properties = new Properties();
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        parameters.properties.forEach(properties::setProperty);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Void> out) {
        try (Consumer<?, ?> consumer = new KafkaConsumer<>(properties)) {
            TopicPartition topicPartition = new TopicPartition(topic, partition);
            consumer.assign(List.of(topicPartition));
            consumer.seek(topicPartition, seekTo);
            consumer.commitSync();
        }
    }

    public static class Parameters {
        public String topic;
        public Integer partition;
        public Long seekTo;
        public Map<String, String> properties = Map.of();
    }
}

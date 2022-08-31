package b.nana.technology.gingester.transformers.kafka;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.ByteArraySerializer;

import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public final class Produce implements Transformer<byte[], byte[]> {

    private final ContextMap<ProducerWrapper> producers = new ContextMap<>();

    private final Template topicTemplate;
    private final FetchKey fetchKey;
    private final Properties properties;

    public Produce(Parameters parameters) {
        topicTemplate = Context.newTemplate(parameters.topic);
        fetchKey = parameters.key;
        properties = new Properties();
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        parameters.properties.forEach(properties::setProperty);
    }

    @Override
    public void prepare(Context context, Receiver<byte[]> out) {
        producers.put(context, new ProducerWrapper(new KafkaProducer<>(properties)));
    }

    @Override
    public void transform(Context context, byte[] in, Receiver<byte[]> out) {

        ProducerRecord<byte[], byte[]> record;

        if (fetchKey == null) {

            record = new ProducerRecord<>(
                    topicTemplate.render(context),
                    in
            );

        } else {

            byte[] key = (byte[]) context.require(fetchKey);

            record = new ProducerRecord<>(
                    topicTemplate.render(context),
                    key,
                    in
            );
        }

        producers.get(context).send(record);
    }

    @Override
    public void finish(Context context, Receiver<byte[]> out) {
        producers.remove(context).close();
    }

    private static class ProducerWrapper implements Callback {

        final Map<String, Integer> exceptionCounters = new TreeMap<>();
        final Producer<byte[], byte[]> producer;

        private ProducerWrapper(Producer<byte[], byte[]> producer) {
            this.producer = producer;
        }

        public void send(ProducerRecord<byte[],byte[]> record) {
            producer.send(record, this);
        }

        @Override
        public void onCompletion(RecordMetadata recordMetadata, Exception e) {
            if (e != null) {
                exceptionCounters.compute(e.getClass().getSimpleName(), (k, v) -> (v == null ? 0 : v) + 1);
            }
        }

        public void close() {
            producer.close();
            if (!exceptionCounters.isEmpty()) {
                StringBuilder message = new StringBuilder("Kafka producer encountered exceptions:\n");
                exceptionCounters.forEach((exceptionClassName, count) -> {
                    message
                            .append(exceptionClassName)
                            .append(": ")
                            .append(count)
                            .append(" times\n");
                });
                message.deleteCharAt(message.length() - 1);
                throw new IllegalStateException(message.toString());
            }
        }
    }

    public static class Parameters {
        public TemplateParameters topic;
        public FetchKey key = new FetchKey("key");
        public Map<String, String> properties = Map.of();
    }
}

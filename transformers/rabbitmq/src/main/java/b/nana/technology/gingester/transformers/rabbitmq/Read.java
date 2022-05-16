package b.nana.technology.gingester.transformers.rabbitmq;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import com.rabbitmq.client.Envelope;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class Read extends RabbitmqTransformer<Object, byte[]> {

    private final boolean autoAck;

    public Read(Parameters parameters) {
        super(parameters);
        autoAck = parameters.autoAck;
    }

    @Override
    public void transform(Context context, Object in, Receiver<byte[]> out) throws Exception {
        AtomicLong counter = new AtomicLong();
        String consumerTag = getChannel().basicConsume(
                getParameters().queue,
                autoAck,
                (ct, delivery) -> {
                    // TODO this happens on a RabbitMQ thread, makes batching impossible
                    Envelope envelope = delivery.getEnvelope();
                    out.accept(context.stash(Map.of(
                            "description", counter.getAndIncrement(),
                            "exchange", envelope.getExchange(),
                            "routingKey", envelope.getRoutingKey(),
                            "deliveryTag", envelope.getDeliveryTag(),
                            "isRedeliver", envelope.isRedeliver()

                    )), delivery.getBody());
                },
                ct -> {}
        );

        // wait for interrupt
        synchronized (this) {
            try {
                while (true) {
                    wait();
                }
            } catch (InterruptedException e) {
                // ignore
            }
        }

        getChannel().basicCancel(consumerTag);
    }

    public static class Parameters extends RabbitmqTransformer.Parameters {
        public boolean autoAck;
    }
}

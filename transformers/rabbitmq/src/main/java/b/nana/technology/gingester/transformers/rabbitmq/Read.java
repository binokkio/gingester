package b.nana.technology.gingester.transformers.rabbitmq;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;

public final class Read extends RabbitmqTransformer<Object, byte[]> {

    public Read(RabbitmqTransformer.Parameters parameters) {
        super(parameters);
    }

    @Override
    public void transform(Context context, Object in, Receiver<byte[]> out) throws Exception {

        getChannel().basicConsume(
                getParameters().queue,
                true,
                (consumerTag, delivery) -> out.accept(context, delivery.getBody()),  // TODO this happens on a RabbitMQ thread, makes batching impossible
                consumerTag -> {}
        );

        // wait for interrupt
        synchronized (this) {
            wait();
        }
    }
}

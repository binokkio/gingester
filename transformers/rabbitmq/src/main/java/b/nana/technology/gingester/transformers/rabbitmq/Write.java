package b.nana.technology.gingester.transformers.rabbitmq;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;

public final class Write extends RabbitmqTransformer<byte[], byte[]> {

    private final String exchange;
    private final String routingKey;

    public Write(Parameters parameters) {
        super(parameters);
        exchange = parameters.exchange;
        routingKey = parameters.routingKey != null ? parameters.routingKey : parameters.queue;
    }

    @Override
    public void open() throws Exception {
        super.open();
        // TODO implement a return listener
    }

    @Override
    public void transform(Context context, byte[] in, Receiver<byte[]> out) throws Exception {

        getChannel().basicPublish(
                exchange,
                routingKey,
                true,
                false,
                null,
                in
        );

        out.accept(context, in);
    }

    public static class Parameters extends RabbitmqTransformer.Parameters {
        public String exchange = "";
        public String routingKey;
    }
}

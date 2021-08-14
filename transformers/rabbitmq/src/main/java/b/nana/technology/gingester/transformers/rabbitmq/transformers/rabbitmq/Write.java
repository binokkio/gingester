package b.nana.technology.gingester.transformers.rabbitmq.transformers.rabbitmq;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.transformers.rabbitmq.common.RabbitmqTransformer;
import com.rabbitmq.client.BasicProperties;
import com.rabbitmq.client.GetResponse;

public class Write extends RabbitmqTransformer<byte[], byte[]> {

    private final String exchange;
    private final String routingKey;

    public Write(Parameters parameters) {
        super(parameters);
        exchange = parameters.exchange;
        routingKey = parameters.routingKey != null ? parameters.routingKey : parameters.queue;
    }

    @Override
    protected void open() throws Exception {
        super.open();
        // TODO implement a return listener
    }

    @Override
    protected void transform(Context context, byte[] input) throws Exception {

        getChannel().basicPublish(
                exchange,
                routingKey,
                true,
                false,
                null,
                input
        );

        emit(context, input);
    }

    public static class Parameters extends RabbitmqTransformer.Parameters {
        public String exchange = "";
        public String routingKey;
    }
}

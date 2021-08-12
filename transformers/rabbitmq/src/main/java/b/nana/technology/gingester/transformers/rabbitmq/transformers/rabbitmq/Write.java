package b.nana.technology.gingester.transformers.rabbitmq.transformers.rabbitmq;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.transformers.rabbitmq.common.RabbitmqTransformer;
import com.rabbitmq.client.BasicProperties;
import com.rabbitmq.client.GetResponse;

public class Write extends RabbitmqTransformer<byte[], byte[]> {

    private final String exchange;
    private final String routingKey;
    private final boolean mandatory;
    private final boolean immediate;

    public Write(Parameters parameters) {
        super(parameters);
        exchange = parameters.exchange;
        routingKey = parameters.routingKey;
        mandatory = parameters.mandatory;
        immediate = parameters.immediate;
    }

    @Override
    protected void transform(Context context, byte[] input) throws Exception {

        getChannel().basicPublish(
                exchange,
                routingKey,
                mandatory,
                immediate,
                null,
                input
        );

        emit(context, input);

        synchronized (this) {
            wait();
        }
    }

    public static class Parameters extends RabbitmqTransformer.Parameters {
        public String exchange = "";
        public String routingKey = "";
        public boolean mandatory;
        public boolean immediate;
    }
}

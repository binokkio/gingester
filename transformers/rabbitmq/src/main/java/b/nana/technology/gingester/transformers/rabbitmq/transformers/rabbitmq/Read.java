package b.nana.technology.gingester.transformers.rabbitmq.transformers.rabbitmq;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.transformers.rabbitmq.common.RabbitmqTransformer;

public class Read extends RabbitmqTransformer<Void, byte[]> {

    public Read(RabbitmqTransformer.Parameters parameters) {
        super(parameters);
    }

    @Override
    protected void transform(Context context, Void input) throws Exception {

        getChannel().basicConsume(
                getParameters().queue,
                true,
                (consumerTag, delivery) -> emit(context, delivery.getBody()),
                consumerTag -> {}
        );

        synchronized (this) {
            wait();
        }
    }
}

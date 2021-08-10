import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.transformers.rabbitmq.common.RabbitmqTransformer;
import b.nana.technology.gingester.transformers.rabbitmq.transformers.rabbitmq.Read;
import org.junit.jupiter.api.Test;

class Tests {

    @Test
    void test() {
        System.out.println(123);

        RabbitmqTransformer.Parameters readParameters = new RabbitmqTransformer.Parameters();
        readParameters.uri = "amqp://localhost";
        readParameters.queue = "foo";
        Read read = new Read(readParameters);

        Gingester.Builder gBuilder = Gingester.newBuilder();
        gBuilder.link(read, bytes -> {
            System.out.println(new String(bytes));
        });

        gBuilder.build().run();
    }
}

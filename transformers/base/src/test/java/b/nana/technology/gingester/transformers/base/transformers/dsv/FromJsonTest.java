package b.nana.technology.gingester.transformers.base.transformers.dsv;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.UniReceiver;
import b.nana.technology.gingester.transformers.base.common.iostream.OutputStreamWrapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FromJsonTest {

    @Test
    void test() throws Exception {

        FromJson.Parameters parameters = new FromJson.Parameters();
        parameters.delimiter = '1';
        parameters.quote = '2';

        FromJson fromJson = new FromJson(parameters);
        Context context = new Context.Builder().build();

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        fromJson.prepare(context, (UniReceiver<OutputStreamWrapper>) o -> o.wrap(output));

        ObjectNode message1 = JsonNodeFactory.instance.objectNode();
        message1.put("message", "Hello, World 1!");
        fromJson.transform(context, message1, null);

        ObjectNode message2 = JsonNodeFactory.instance.objectNode();
        message2.put("message", "Hello, World 2!");
        fromJson.transform(context, message2, null);

        ObjectNode message3 = JsonNodeFactory.instance.objectNode();
        message3.put("message", "Hello, World 3!");
        fromJson.transform(context, message3, null);

        fromJson.finish(context, null);

        assertEquals(
                "message\n" +
                "2Hello, World 1!2\n" +
                "2Hello, World 22!2\n" +
                "Hello, World 3!\n",
                output.toString(StandardCharsets.UTF_8)
        );
    }
}

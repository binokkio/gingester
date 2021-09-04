package b.nana.technology.gingester.transformers.base;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.configuration.Parameters;
import b.nana.technology.gingester.core.receiver.ConsumerReceiver;
import b.nana.technology.gingester.transformers.base.transformers.GenerateString;
import b.nana.technology.gingester.transformers.base.transformers.StringAppend;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GenerateStringTest {

    @Test
    void testGenerateString() {

        GenerateString.Parameters parameters = new GenerateString.Parameters();
        parameters.string = "Hello, World!";
        parameters.count = 3;

        GenerateString generateString = new GenerateString(parameters);

        Queue<String> result = new ArrayDeque<>();

        generateString.transform(null, null, (ConsumerReceiver<String>) result::add);

        assertEquals(3, result.size());
        assertEquals("Hello, World!", result.remove());
        assertEquals("Hello, World!", result.remove());
        assertEquals("Hello, World!", result.remove());
    }

    @Test
    void testGenerateStringAppend() {

        Gingester gingester = new Gingester();
        Queue<String> result = new ArrayDeque<>();

        GenerateString.Parameters parameters = new GenerateString.Parameters();
        parameters.string = "Hello, World!";
        parameters.count = 3;

        Parameters async = new Parameters();
        async.setAsync(true);

        gingester.add(new GenerateString(parameters));
        gingester.add(new StringAppend(), async);
        gingester.add(new StringAppend());
        gingester.add(result::add);

        gingester.run();

        assertEquals(3, result.size());
        assertEquals("Hello, World!!!", result.remove());
        assertEquals("Hello, World!!!", result.remove());
        assertEquals("Hello, World!!!", result.remove());
    }
}
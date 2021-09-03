package b.nana.technology.transformers.base;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.controller.Controller;
import b.nana.technology.gingester.core.receiver.SimpleReceiver;
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

        generateString.transform(null, null, (SimpleReceiver<String>) result::add);

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

        Controller.Parameters async = new Controller.Parameters();
        async.async = true;

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
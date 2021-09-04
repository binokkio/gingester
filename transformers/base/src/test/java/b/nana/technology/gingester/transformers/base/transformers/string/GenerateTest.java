package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.receiver.UniReceiver;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GenerateTest {

    @Test
    void testStringGenerate() throws InterruptedException {

        Generate.Parameters parameters = new Generate.Parameters();
        parameters.string = "Hello, World!";
        parameters.count = 3;

        Generate generate = new Generate(parameters);

        Queue<String> result = new ArrayDeque<>();

        generate.transform(Context.newSeed(), null, (UniReceiver<String>) result::add);

        assertEquals(3, result.size());
        assertEquals("Hello, World!", result.remove());
        assertEquals("Hello, World!", result.remove());
        assertEquals("Hello, World!", result.remove());
    }
}
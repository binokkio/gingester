package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.UniReceiver;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreateTest {

    @Test
    void testStringCreate() throws InterruptedException {

        Queue<String> result = new ArrayDeque<>();

        Create.Parameters parameters = new Create.Parameters();
        parameters.template = "Hello, World!";
        parameters.count = 3;

        Create create = new Create(parameters);
        create.transform(new Context.Builder().build(null), null, (UniReceiver<String>) result::add);

        assertEquals(3, result.size());
        assertEquals("Hello, World!", result.remove());
        assertEquals("Hello, World!", result.remove());
        assertEquals("Hello, World!", result.remove());
    }

    @Test
    void testStringCreateTemplating() throws InterruptedException {

        Queue<String> result = new ArrayDeque<>();

        Create.Parameters parameters = new Create.Parameters();
        parameters.template = "Hello, ${target}!";
        parameters.count = 3;

        Create create = new Create(parameters);
        create.transform(new Context.Builder().stash("target", "World").build(null), null, (UniReceiver<String>) result::add);

        assertEquals(3, result.size());
        assertEquals("Hello, World!", result.remove());
        assertEquals("Hello, World!", result.remove());
        assertEquals("Hello, World!", result.remove());
    }
}
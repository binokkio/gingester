package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.UniReceiver;
import b.nana.technology.gingester.core.template.TemplateParameters;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreateTest {

    @Test
    void testStringCreate() throws InterruptedException {

        Queue<String> result = new ArrayDeque<>();

        Create.Parameters parameters = new Create.Parameters();
        parameters.template = new TemplateParameters("Hello, World!");

        Create create = new Create(parameters);
        create.transform(Context.newTestContext(), null, (UniReceiver<String>) result::add);

        assertEquals(1, result.size());
        assertEquals("Hello, World!", result.remove());
    }

    @Test
    void testStringCreateTemplating() throws InterruptedException {

        Queue<String> result = new ArrayDeque<>();

        Create.Parameters parameters = new Create.Parameters();
        parameters.template = new TemplateParameters("Hello, ${target}!");

        Create create = new Create(parameters);
        create.transform(Context.newTestContext().stash("target", "World").buildForTesting(), null, (UniReceiver<String>) result::add);

        assertEquals(1, result.size());
        assertEquals("Hello, World!", result.remove());
    }
}
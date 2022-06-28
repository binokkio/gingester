package b.nana.technology.gingester.core.transformers.string;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.UniReceiver;
import b.nana.technology.gingester.core.template.TemplateParameters;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void testStringCreateTemplatingNumberFormat() {

        AtomicReference<String> result = new AtomicReference<>();

        new Gingester().cli("" +
                "-t IntDef 123456 " +
                "-s number " +
                "-t StringCreate 'Hello, ${number}!'")
                .attach(result::set)
                .run();

        assertEquals("Hello, 123456!", result.get());
    }

    @Test
    void testStringCreateTemplatingBooleanFormat() {

        AtomicReference<String> result = new AtomicReference<>();

        new Gingester().cli("" +
                "-t BooleanDef true " +
                "-s value " +
                "-t StringCreate 'Hello, ${value}!'")
                .attach(result::set)
                .run();

        assertEquals("Hello, true!", result.get());
    }
}
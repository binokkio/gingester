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

class DefTest {

    @Test
    void testStringDef() throws InterruptedException {

        Queue<String> result = new ArrayDeque<>();

        Def.Parameters parameters = new Def.Parameters();
        parameters.template = new TemplateParameters("Hello, World!");

        Def create = new Def(parameters);
        create.transform(Context.newTestContext(), null, (UniReceiver<String>) result::add);

        assertEquals(1, result.size());
        assertEquals("Hello, World!", result.remove());
    }

    @Test
    void testStringDefTemplating() throws InterruptedException {

        Queue<String> result = new ArrayDeque<>();

        Def.Parameters parameters = new Def.Parameters();
        parameters.template = new TemplateParameters("Hello, ${target}!");

        Def create = new Def(parameters);
        create.transform(Context.newTestContext().stash("target", "World").buildForTesting(), null, (UniReceiver<String>) result::add);

        assertEquals(1, result.size());
        assertEquals("Hello, World!", result.remove());
    }

    @Test
    void testStringDefTemplatingNumberFormat() {

        AtomicReference<String> result = new AtomicReference<>();

        new Gingester().cli("" +
                "-t IntDef 123456 " +
                "-s number " +
                "-t StringDef 'Hello, ${number}!'")
                .attach(result::set)
                .run();

        assertEquals("Hello, 123456!", result.get());
    }

    @Test
    void testStringDefTemplatingBooleanFormat() {

        AtomicReference<String> result = new AtomicReference<>();

        new Gingester().cli("" +
                "-t BooleanDef true " +
                "-s value " +
                "-t StringDef 'Hello, ${value}!'")
                .attach(result::set)
                .run();

        assertEquals("Hello, true!", result.get());
    }
}
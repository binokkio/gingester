package b.nana.technology.gingester.core.transformers.string;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.UniReceiver;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformers.primitive.StringDef;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class StringDefTest {

    @Test
    void testStringDef() throws InterruptedException {

        Queue<String> result = new ArrayDeque<>();

        StringDef.Parameters parameters = new StringDef.Parameters();
        parameters.template = new TemplateParameters("Hello, World!");

        StringDef create = new StringDef(parameters);
        create.transform(Context.newTestContext(), null, (UniReceiver<String>) result::add);

        assertEquals(1, result.size());
        assertEquals("Hello, World!", result.remove());
    }

    @Test
    void testStringDefTemplating() throws InterruptedException {

        Queue<String> result = new ArrayDeque<>();

        StringDef.Parameters parameters = new StringDef.Parameters();
        parameters.template = new TemplateParameters("Hello, ${target}!");

        StringDef create = new StringDef(parameters);
        create.transform(Context.newTestContext().stash("target", "World").buildForTesting(), null, (UniReceiver<String>) result::add);

        assertEquals(1, result.size());
        assertEquals("Hello, World!", result.remove());
    }

    @Test
    void testStringDefTemplatingNumberFormat() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t IntDef 123456 " +
                "-s number " +
                "-t StringDef 'Hello, ${number}!'")
                .add(result::set)
                .run();

        assertEquals("Hello, 123456!", result.get());
    }

    @Test
    void testStringDefTemplatingBooleanFormat() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t BooleanDef true " +
                "-s value " +
                "-t StringDef 'Hello, ${value}!'")
                .add(result::set)
                .run();

        assertEquals("Hello, true!", result.get());
    }

    @Test
    void testStringDefResourceWithKwargs() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef '{template: \"/template.ftl\", is: \"RESOURCE\", kwargs: {\"target\": \"World\"}}'")
                .add(result::set)
                .run();

        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testStringDefFullParameters() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef '{template: {template: \"Hello, World!\", is: \"STRING\"}}'")
                .add(result::set)
                .run();

        assertEquals("Hello, World!", result.get());
    }
}
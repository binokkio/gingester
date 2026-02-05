package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.core.receiver.UniReceiver;
import b.nana.technology.gingester.core.template.TemplateParameters;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AppendTest {

    @Test
    void testStringAppend() {
        Append.Parameters parameters = new Append.Parameters(new TemplateParameters("!"));
        Append append = new Append(parameters);
        Queue<String> result = new ArrayDeque<>();
        append.transform(null, "Hello, World", (UniReceiver<String>) result::add);
        assertEquals(1, result.size());
        assertEquals("Hello, World!", result.remove());
    }

    @Test
    void testCliVariations() {

        AtomicReference<String> r1 = new AtomicReference<>();
        new FlowBuilder().cli("""
                -t StringDef 'Hello, World'
                -t StringAppend '{append:{template:"!"}}'
                """)
                .add(r1::set)
                .run();

        AtomicReference<String> r2 = new AtomicReference<>();
        new FlowBuilder().cli("""
                -t StringDef 'Hello, World'
                -t StringAppend '{template:"!"}'
                """)
                .add(r2::set)
                .run();

        AtomicReference<String> r3 = new AtomicReference<>();
        new FlowBuilder().cli("""
                -t StringDef 'Hello, World'
                -t StringAppend '!'
                """)
                .add(r3::set)
                .run();

        assertEquals("Hello, World!", r1.get());
        assertEquals("Hello, World!", r2.get());
        assertEquals("Hello, World!", r3.get());
    }
}
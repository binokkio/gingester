package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformers.primitive.LongDef;
import b.nana.technology.gingester.core.transformers.stash.Fetch;
import b.nana.technology.gingester.core.transformers.stash.Stash;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FlowBuilderTest {

    @Test
    void test() {

        AtomicReference<Long> result = new AtomicReference<>();

        new FlowBuilder()

                .node()
                .transformer(new LongDef(new LongDef.Parameters(10L)))
                .add()

                .node()
                .transformer(new Stash(new Stash.Parameters("hello")))
                .add()

                .node()
                .transformer(new Fetch(new Fetch.Parameters("hello")))
                .add()

                .add(result::set)

                .run();

        assertEquals(10, result.get());
    }

    @Test
    void testDivertConvergingAsymmetricFlow() {

        Deque<String> results = new ArrayDeque<>();

        new FlowBuilder()
                .cli("-t StringDef 'Hello, World!' -l A B -pt A -pt -l C -pt B -pt -pt -l C -pt C")
                .divert(List.of("A", "B"))
                .add(results::add)
                .run();

        assertEquals(2, results.size());
        assertEquals("Hello, World!", results.remove());
        assertEquals("Hello, World!", results.remove());
    }

    @Test
    void testReplace() {

        Deque<String> results = new ArrayDeque<>();

        new FlowBuilder()
                .cli(getClass().getResource("/hello-world-diamond.cli"))
                .replace("Emphasize", (Transformer<String, String>) (context, in, out) -> out.accept(context, (in + "!!!")))
                .add(results::add)
                .run();

        assertEquals("Hello, World!!!", results.remove());
        assertEquals("Hello, World?", results.remove());
    }
}
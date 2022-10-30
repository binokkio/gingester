package b.nana.technology.gingester.transformers.groovy;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.core.transformers.passthrough.ConsumerPassthrough;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TernaryRouteTest {

    @Test
    void testTernaryRouteThen() {

        AtomicReference<String> thenResult = new AtomicReference<>();
        AtomicReference<String> otherwiseResult = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-s " +
                "-t TernaryRoute '\"${stash}\".contains(\"Hello\")' Then Otherwise")
                .node().id("Then").transformer(new ConsumerPassthrough<>(thenResult::set)).add()
                .cli("--")
                .node().id("Otherwise").transformer(new ConsumerPassthrough<>(otherwiseResult::set)).add()
                .run();

        assertEquals("Hello, World!", thenResult.get());
        assertNull(otherwiseResult.get());
    }

    @Test
    void testTernaryRouteOtherwise() {

        AtomicReference<String> thenResult = new AtomicReference<>();
        AtomicReference<String> otherwiseResult = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'Bye, World!' " +
                "-s " +
                "-t TernaryRoute '\"${stash}\".contains(\"Hello\")' Then Otherwise")
                .node().id("Then").transformer(new ConsumerPassthrough<>(thenResult::set)).add()
                .cli("--")
                .node().id("Otherwise").transformer(new ConsumerPassthrough<>(otherwiseResult::set)).add()
                .run();

        assertNull(thenResult.get());
        assertEquals("Bye, World!", otherwiseResult.get());
    }
}
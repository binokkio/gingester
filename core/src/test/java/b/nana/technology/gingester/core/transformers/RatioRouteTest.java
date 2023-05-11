package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RatioRouteTest {

    @Test
    void test() {
        Deque<String> aResults = new ArrayDeque<>();
        Deque<String> bResults = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-t Repeat 6 " +
                "-t StringDef '${description}' " +
                "-t RatioRoute 2 A 3 B " +
                "-t A:Passthrough -- " +
                "-t B:Passthrough --")
                .addTo(aResults::add, "A")
                .addTo(bResults::add, "B")
                .run();

        assertEquals("0", aResults.remove());
        assertEquals("1", aResults.remove());
        assertEquals("2", bResults.remove());
        assertEquals("3", bResults.remove());
        assertEquals("4", bResults.remove());
        assertEquals("5", aResults.remove());
    }
}
package b.nana.technology.gingester.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.*;

class DiamondRouteTest {

    @Test
    void test() {

        ArrayDeque<String> results = new ArrayDeque<>();

        FlowBuilder flowBuilder = new FlowBuilder().cli("-cr hello-world-diamond.cli");
        flowBuilder.add(results::add);
        flowBuilder.run();

        assertEquals(2, results.size());
        assertEquals("Hello, World!", results.remove());
        assertEquals("Hello, World?", results.remove());
    }

    @Test
    void testAddToTarget() {

        ArrayDeque<String> results = new ArrayDeque<>();

        FlowBuilder flowBuilder = new FlowBuilder().cli("-cr hello-world-diamond.cli");
        flowBuilder.addTo(results::add, "Emphasize");
        flowBuilder.run();

        assertEquals(1, results.size());
        assertEquals("Hello, World!", results.remove());
    }

    @Test
    void testAddToTargets() {

        ArrayDeque<String> emphasizeResults = new ArrayDeque<>();
        ArrayDeque<String> questionResults = new ArrayDeque<>();

        FlowBuilder flowBuilder = new FlowBuilder().cli("-cr hello-world-diamond.cli");
        flowBuilder.addTo(emphasizeResults::add, "Emphasize");
        flowBuilder.addTo(questionResults::add, "Question");
        flowBuilder.run();

        assertEquals(1, emphasizeResults.size());
        assertEquals("Hello, World!", emphasizeResults.remove());

        assertEquals(1, questionResults.size());
        assertEquals("Hello, World?", questionResults.remove());
    }
}

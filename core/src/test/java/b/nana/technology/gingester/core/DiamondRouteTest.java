package b.nana.technology.gingester.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.*;

class DiamondRouteTest {

    @Test
    void test() {

        ArrayDeque<String> results = new ArrayDeque<>();

        Gingester gingester = new Gingester().cli("-cr hello-world-diamond.cli");
        gingester.attach(results::add);
        gingester.run();

        assertEquals(2, results.size());
        assertEquals("Hello, World!", results.remove());
        assertEquals("Hello, World?", results.remove());
    }

    @Test
    void testAttachToTarget() {

        ArrayDeque<String> results = new ArrayDeque<>();

        Gingester gingester = new Gingester().cli("-cr hello-world-diamond.cli");
        gingester.attach(results::add, "Emphasize");
        gingester.run();

        assertEquals(1, results.size());
        assertEquals("Hello, World!", results.remove());
    }

    @Test
    void testAttachToTargets() {

        ArrayDeque<String> emphasizeResults = new ArrayDeque<>();
        ArrayDeque<String> questionResults = new ArrayDeque<>();

        Gingester gingester = new Gingester().cli("-cr hello-world-diamond.cli");
        gingester.attach(emphasizeResults::add, "Emphasize");
        gingester.attach(questionResults::add, "Question");
        gingester.run();

        assertEquals(1, emphasizeResults.size());
        assertEquals("Hello, World!", emphasizeResults.remove());

        assertEquals(1, questionResults.size());
        assertEquals("Hello, World?", questionResults.remove());
    }
}

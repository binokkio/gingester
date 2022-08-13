package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.*;

class DivertTest {

    @Test
    void testDivertEmphasize() {

        ArrayDeque<String> results = new ArrayDeque<>();

        new FlowBuilder()
                .cli("-cr hello-world-diamond.cli")
                .divert("Emphasize")
                .add(results::add)
                .run();

        assertEquals(1, results.size());
        assertEquals("Hello, World!", results.remove());
    }

    @Test
    void testDivertQuestion() {

        ArrayDeque<String> results = new ArrayDeque<>();

        new FlowBuilder()
                .cli("-cr hello-world-diamond.cli")
                .divert("Question")
                .add(results::add)
                .run();

        assertEquals(1, results.size());
        assertEquals("Hello, World?", results.remove());
    }
}

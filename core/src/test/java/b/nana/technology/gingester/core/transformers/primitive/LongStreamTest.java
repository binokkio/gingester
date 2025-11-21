package b.nana.technology.gingester.core.transformers.primitive;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LongStreamTest {

    @Test
    void testFromZero() {

        ArrayDeque<Long> results = new ArrayDeque<>();

        new FlowBuilder().cli("-t LongStream 3")
                .add(results::add)
                .run();

        assertEquals(0, results.pop());
        assertEquals(1, results.pop());
        assertEquals(2, results.pop());
        assertTrue(results.isEmpty());
    }

    @Test
    void testFromStart() {

        ArrayDeque<Long> results = new ArrayDeque<>();

        new FlowBuilder().cli("-t LongStream 1 3")
                .add(results::add)
                .run();

        assertEquals(1, results.pop());
        assertEquals(2, results.pop());
        assertTrue(results.isEmpty());
    }
}
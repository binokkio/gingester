package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.*;

class CycleRouteTest {

    @Test
    void test() {
        Deque<String> results = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-t Repeat 3 " +
                "-t StringDef '${description}' " +
                "-t CycleRoute A B " +
                "-t B:Void -- " +
                "-t A:Passthrough")
                .add(results::add)
                .run();

        assertEquals("0", results.remove());
        assertEquals("2", results.remove());
    }
}
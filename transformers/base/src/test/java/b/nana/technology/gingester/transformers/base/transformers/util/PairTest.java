package b.nana.technology.gingester.transformers.base.transformers.util;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.UniReceiver;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PairTest {

    @Test
    void test() {

        ArrayDeque<String> result = new ArrayDeque<>();

        new FlowBuilder().cli("""
                -t Repeat 5
                -f description
                -t Pair
                -t StringDef '${a}-${b}'
                -t StdOut
                """)
                .add(result::add)
                .run();

        assertEquals("0-1", result.removeFirst());
        assertEquals("1-2", result.removeFirst());
        assertEquals("2-3", result.removeFirst());
        assertEquals("3-4", result.removeFirst());
    }
}
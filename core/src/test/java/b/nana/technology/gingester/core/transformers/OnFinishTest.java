package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.*;

class OnFinishTest {

    @Test
    void test() {

        Deque<Integer> results = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-e Void -t Void -- " +
                "-sft Repeat 3 " +
                "-t Monkey " +
                "-stt OnFinish flawless " +
                "-f description")
                .attach(results::add)
                .run();

        assertEquals(0, results.remove());
        assertEquals(2, results.remove());
        assertTrue(results.isEmpty());
    }
}
package b.nana.technology.gingester.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OnFlawlessTest {

    @Test
    void testTwoOutOfThree() {

        Gingester gingester = new Gingester().cli("" +
                "-e Void -t Void -- " +
                "-t Repeat 3 " +
                "-sft Generate 'Hello, World!' " +
                "-t Monkey " +
                "-stt OnFinish flawless " +
                "-f description");

        ArrayDeque<Integer> results = new ArrayDeque<>();
        gingester.attach(results::add);

        gingester.run();

        assertEquals(0, results.remove());
        assertEquals(2, results.remove());
    }
}

package b.nana.technology.gingester.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void testBubblingWithoutCatcher() {

        AtomicReference<String> seedFlawlessResult = new AtomicReference<>();
        AtomicReference<String> seedFlawedResult = new AtomicReference<>();

        new Gingester().cli("" +
                "-e Void -t Void -- " +
                "-t Repeat 3 " +
                "-t Monkey 2 " +
                "-l SeedFlawless SeedFlawed " +
                "-t SeedFlawless:OnFinish flawless -- " +
                "-t SeedFlawed:OnFinish flawed -- ")
                .attach(seedFlawlessResult::set, "SeedFlawless")
                .attach(seedFlawedResult::set, "SeedFlawed")
                .run();

        assertNull(seedFlawlessResult.get());
        assertEquals("finish signal", seedFlawedResult.get());
    }

    @Test
    void testBubblingWithCatcher() {

        Deque<String> seedFlawlessResults = new ConcurrentLinkedDeque<>();
        Deque<String> seedFlawedResults = new ConcurrentLinkedDeque<>();
        Deque<String> repeatFlawlessResults = new ConcurrentLinkedDeque<>();
        Deque<String> repeatFlawedResults = new ConcurrentLinkedDeque<>();
        Deque<String> passthroughFlawlessResults = new ConcurrentLinkedDeque<>();
        Deque<String> passthroughFlawedResults = new ConcurrentLinkedDeque<>();
        Deque<String> monkeyFlawlessResults = new ConcurrentLinkedDeque<>();
        Deque<String> monkeyFlawedResults = new ConcurrentLinkedDeque<>();

        new Gingester().cli("" +
                "-t Void -- " +
                "-t Repeat 3 " +
                "-e Void " +
                "-t Passthrough " +
                "-t Monkey 2 " +
                "-l SeedFlawless SeedFlawed RepeatFlawless RepeatFlawed PassthroughFlawless PassthroughFlawed MonkeyFlawless MonkeyFlawed " +
                "-sf __seed__ -stt SeedFlawless:OnFinish flawless -t SeedFlawlessResult:Passthrough -- " +
                "-sf __seed__ -stt SeedFlawed:OnFinish flawed -t SeedFlawedResult:Passthrough -- " +
                "-sf Repeat -stt RepeatFlawless:OnFinish flawless -t RepeatFlawlessResult:Generate '${description}' -- " +
                "-sf Repeat -stt RepeatFlawed:OnFinish flawed -t RepeatFlawedResult:Generate '${description}' -- " +
                "-sf Passthrough -stt PassthroughFlawless:OnFinish flawless -t PassthroughFlawlessResult:Generate '${description}' -- " +
                "-sf Passthrough -stt PassthroughFlawed:OnFinish flawed -t PassthroughFlawedResult:Generate '${description}' -- " +
                "-sf Monkey -stt MonkeyFlawless:OnFinish flawless -t MonkeyFlawlessResult:Generate '${description}' -- " +
                "-sf Monkey -stt MonkeyFlawed:OnFinish flawed -t MonkeyFlawedResult:Generate '${description}' -- ")
                .attach(seedFlawlessResults::add, "SeedFlawlessResult")
                .attach(seedFlawedResults::add, "SeedFlawedResult")
                .attach(repeatFlawlessResults::add, "RepeatFlawlessResult")
                .attach(repeatFlawedResults::add, "RepeatFlawedResult")
                .attach(passthroughFlawlessResults::add, "PassthroughFlawlessResult")
                .attach(passthroughFlawedResults::add, "PassthroughFlawedResult")
                .attach(monkeyFlawlessResults::add, "MonkeyFlawlessResult")
                .attach(monkeyFlawedResults::add, "MonkeyFlawedResult")
                .run();

        assertEquals(1, seedFlawlessResults.size());  // 1 because the monkey exceptions don't bubble past Repeat
        assertEquals(0, seedFlawedResults.size());  // 0 because the monkey exceptions don't bubble past Repeat
        assertEquals(2, repeatFlawlessResults.size());  // 2 because the monkey exceptions bubble to here
        assertEquals(1, repeatFlawedResults.size());  // 1 because the monkey exceptions bubble to here
        assertEquals(2, passthroughFlawlessResults.size());  // 2 because the monkey exceptions bubble past here
        assertEquals(1, passthroughFlawedResults.size());  // 1 because the monkey exceptions bubble past here
        assertEquals(2, monkeyFlawlessResults.size());  // 2 because the monkey passed through 2 inputs
        assertEquals(0, monkeyFlawedResults.size());  // 0 because the input for which the monkey threw has no output
    }
}

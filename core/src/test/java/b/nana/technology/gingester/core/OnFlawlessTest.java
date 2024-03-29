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

        ArrayDeque<Integer> results = new ArrayDeque<>();

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-t Void -- " +
                "-t Repeat 3 " +
                "-sft StringDef 'Hello, World!' " +
                "-e Void " +
                "-t Monkey " +
                "-stt OnFinish flawless " +
                "-f description");

        flowBuilder.add(results::add);
        flowBuilder.run();

        assertEquals(0, results.remove());
        assertEquals(2, results.remove());
    }

    @Test
    void testBubblingWithoutCatcher() {

        AtomicReference<String> seedFlawlessResult = new AtomicReference<>();
        AtomicReference<String> seedFlawedResult = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-e Void -t Void -- " +
                "-t Repeat 3 " +
                "-t Monkey 2 " +
                "-l SeedFlawless SeedFlawed " +
                "-t SeedFlawless:OnFinish flawless -- " +
                "-t SeedFlawed:OnFinish flawed -- ")
                .addTo(seedFlawlessResult::set, "SeedFlawless")
                .addTo(seedFlawedResult::set, "SeedFlawed")
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

        new FlowBuilder().cli("" +
                "-t Void -- " +
                "-t Repeat 3 " +
                "-e Void " +
                "-t Passthrough " +
                "-t Monkey 2 " +
                "-l SeedFlawless SeedFlawed RepeatFlawless RepeatFlawed PassthroughFlawless PassthroughFlawed MonkeyFlawless MonkeyFlawed " +
                "-sf __seed__ -stt SeedFlawless:OnFinish flawless -t SeedFlawlessResult:Passthrough -- " +
                "-sf __seed__ -stt SeedFlawed:OnFinish flawed -t SeedFlawedResult:Passthrough -- " +
                "-sf Repeat -stt RepeatFlawless:OnFinish flawless -t RepeatFlawlessResult:StringDef '${description}' -- " +
                "-sf Repeat -stt RepeatFlawed:OnFinish flawed -t RepeatFlawedResult:StringDef '${description}' -- " +
                "-sf Passthrough -stt PassthroughFlawless:OnFinish flawless -t PassthroughFlawlessResult:StringDef '${description}' -- " +
                "-sf Passthrough -stt PassthroughFlawed:OnFinish flawed -t PassthroughFlawedResult:StringDef '${description}' -- " +
                "-sf Monkey -stt MonkeyFlawless:OnFinish flawless -t MonkeyFlawlessResult:StringDef '${description}' -- " +
                "-sf Monkey -stt MonkeyFlawed:OnFinish flawed -t MonkeyFlawedResult:StringDef '${description}' -- ")
                .addTo(seedFlawlessResults::add, "SeedFlawlessResult")
                .addTo(seedFlawedResults::add, "SeedFlawedResult")
                .addTo(repeatFlawlessResults::add, "RepeatFlawlessResult")
                .addTo(repeatFlawedResults::add, "RepeatFlawedResult")
                .addTo(passthroughFlawlessResults::add, "PassthroughFlawlessResult")
                .addTo(passthroughFlawedResults::add, "PassthroughFlawedResult")
                .addTo(monkeyFlawlessResults::add, "MonkeyFlawlessResult")
                .addTo(monkeyFlawedResults::add, "MonkeyFlawedResult")
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

package b.nana.technology.gingester.transformers.base.transformers.groupby;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class EqualsTest {

    @Test
    void test() {

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-t Repeat 25 " +
                "-t StringDef 'Hello, World ${description?string.computer[0..0]}!' " +
                "-sft GroupByEquals " +
                "-stt InputStreamJoin " +
                "-t InputStreamToString");

        ArrayDeque<String> result = new ArrayDeque<>();
        flowBuilder.add(result::add);

        flowBuilder.run();

        assertEquals(10, result.size());
        assertEquals(25, result.stream().mapToInt(s -> s.split("!").length).sum());
    }

    @Test
    void testPureBridgeAfterFetchGroupKey() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-sft GroupByEquals customKey " +  // customKey is type String
                "-stt InputStreamJoin " +
                "-t InputStreamDrain " +
                "-f customKey " +
                "-t BytesToString")  // Pure StringToBytes must be inserted
                .add(result::set)
                .run();

        assertEquals("Hello, World!", result.get());
    }

    @Test
    void testLimit() {

        // without limit, GroupByEquals does not close a group until its sync-from finish signal (seed in this case)
        ArrayDeque<String> resultsWithoutLimit = new ArrayDeque<>();
        new FlowBuilder().cli("" +
                "-t Repeat 2 -t StringDef hello -l GroupByEquals " +
                "-t Delay 50 -t Repeat 2 -t Bye:StringDef bye -l GroupByEquals " +
                "-sft GroupByEquals " +
                "-stt InputStreamJoin ', ' " +
                "-t StringAppend '!'")
                .addTo(resultsWithoutLimit::add, "Bye")
                .add(resultsWithoutLimit::add)
                .run();
        assertEquals("bye", resultsWithoutLimit.remove());
        assertEquals("bye", resultsWithoutLimit.remove());
        // the order of the remaining 2 is undetermined due to the GroupByEquals HashMap


        // with limit, GroupByEquals will close each group as soon as the limit is reached
        ArrayDeque<String> resultsWithLimit = new ArrayDeque<>();
        new FlowBuilder().cli("" +
                "-t Repeat 2 -t StringDef hello -l GroupByEquals " +
                "-t Delay 50 -t Repeat 2 -t Bye:StringDef bye -l GroupByEquals " +
                "-sft GroupByEquals 2 " +
                "-stt InputStreamJoin ', ' " +
                "-t StringAppend '!'")
                .addTo(resultsWithLimit::add, "Bye")
                .addTo(resultsWithLimit::add, "StringAppend")
                .run();
        assertEquals("hello, hello!", resultsWithLimit.remove());
        assertEquals("bye", resultsWithLimit.remove());
        assertEquals("bye", resultsWithLimit.remove());
        assertEquals("bye, bye!", resultsWithLimit.getFirst());
    }
}
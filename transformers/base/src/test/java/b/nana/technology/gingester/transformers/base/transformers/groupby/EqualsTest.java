package b.nana.technology.gingester.transformers.base.transformers.groupby;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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

        Deque<String> result = new ConcurrentLinkedDeque<>();
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
    void testMaxEntries() {

        // without maxEntries, GroupByEquals does not close a group until its sync-from finish signal (seed in this case)
        Deque<String> resultsWithoutMaxEntries = new ConcurrentLinkedDeque<>();
        new FlowBuilder().cli("" +
                "-t Repeat 2 -t StringDef hello -l GroupByEquals " +
                "-t Delay 50 -t Repeat 2 -t Bye:StringDef bye -l GroupByEquals " +
                "-sft GroupByEquals " +
                "-stt InputStreamJoin ', ' " +
                "-t StringAppend '!'")
                .addTo(resultsWithoutMaxEntries::add, "Bye")
                .add(resultsWithoutMaxEntries::add)
                .run();
        assertEquals("bye", resultsWithoutMaxEntries.remove());
        assertEquals("bye", resultsWithoutMaxEntries.remove());
        // the order of the remaining 2 is undetermined due to the GroupByEquals HashMap


        // with maxEntries, GroupByEquals will close each group as soon as maxEntries is reached
        Deque<String> resultsWithMaxEntries = new ConcurrentLinkedDeque<>();
        new FlowBuilder().cli("" +
                "-t Repeat 2 -t StringDef hello -l GroupByEquals " +
                "-t Delay 50 -t Repeat 2 -t Bye:StringDef bye -l GroupByEquals " +
                "-sft GroupByEquals 2 " +
                "-stt InputStreamJoin ', ' " +
                "-t StringAppend '!'")
                .addTo(resultsWithMaxEntries::add, "Bye")
                .addTo(resultsWithMaxEntries::add, "StringAppend")
                .run();
        assertEquals("hello, hello!", resultsWithMaxEntries.remove());
        assertEquals("bye", resultsWithMaxEntries.remove());
        assertEquals("bye, bye!", resultsWithMaxEntries.remove());
        assertEquals("bye", resultsWithMaxEntries.remove());
    }

    @Test
    void testMaxGroups() {

        // without maxGroups
        Deque<String> resultsWithoutMaxGroups = new ConcurrentLinkedDeque<>();
        new FlowBuilder().cli("" +
                "-t Repeat 8 " +
                "-t Cycle A B A C -t ObjectToString " +
                "-sft GroupByEquals " +
                "-stt InputStreamJoin ', ' " +
                "-t StringAppend '!'")
                .add(resultsWithoutMaxGroups::add)
                .run();
        List<String> resultsWithoutMaxGroupsSorted = resultsWithoutMaxGroups.stream().sorted(String::compareTo).collect(Collectors.toList());
        assertEquals(3, resultsWithoutMaxGroupsSorted.size());
        assertEquals("A, A, A, A!", resultsWithoutMaxGroupsSorted.get(0));
        assertEquals("B, B!", resultsWithoutMaxGroupsSorted.get(1));
        assertEquals("C, C!", resultsWithoutMaxGroupsSorted.get(2));

        // with maxGroups
        Deque<String> resultsWithMaxGroups = new ConcurrentLinkedDeque<>();
        new FlowBuilder().cli("" +
                "-t Repeat 8 " +
                "-t Cycle A B A C -t ObjectToString " +
                "-sft GroupByEquals '{maxGroups: 2}' " +
                "-stt InputStreamJoin ', ' " +
                "-t StringAppend '!'")
                .add(resultsWithMaxGroups::add)
                .run();
        List<String> resultsWithMaxGroupsSorted = resultsWithMaxGroups.stream().sorted(String::compareTo).collect(Collectors.toList());
        assertEquals(5, resultsWithMaxGroupsSorted.size());
        assertEquals("A, A, A, A!", resultsWithMaxGroupsSorted.get(0));
        assertEquals("B!", resultsWithMaxGroupsSorted.get(1));
        assertEquals("B!", resultsWithMaxGroupsSorted.get(2));
        assertEquals("C!", resultsWithMaxGroupsSorted.get(3));
        assertEquals("C!", resultsWithMaxGroupsSorted.get(4));
    }
}
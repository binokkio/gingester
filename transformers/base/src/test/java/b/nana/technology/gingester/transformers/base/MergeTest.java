package b.nana.technology.gingester.transformers.base;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MergeTest {

    @Test
    void testBasic() {

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-t Passthrough " +
                "-l A B " +
                "-t A:StringDef HelloWorld -s hello -l Merge " +
                "-t B:StringDef ByeWorld -s bye -l Merge " +
                "-t Merge \"['hello', 'bye']\" " +
                "-t StringDef '${hello},${bye}'");

        AtomicReference<String> result = new AtomicReference<>();
        flowBuilder.add(result::set);

        flowBuilder.run();

        assertEquals("HelloWorld,ByeWorld", result.get());
    }

    @Test
    void testList() {

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-t Passthrough " +
                "-l A B " +
                "-t A:StringDef HelloWorld -s message -l Merge " +
                "-t B:StringDef ByeWorld -s message -l Merge " +
                "-t Merge [{fetch:'message',stash:'messages',collect:\"[array]\"}] " +
                "-f messages " +
                "-t ListStream " +
                "-t StringToInputStream " +
                "-t InputStreamJoin , " +
                "-t InputStreamToString");

        AtomicReference<String> result = new AtomicReference<>();
        flowBuilder.add(result::set);

        flowBuilder.run();

        assertEquals("HelloWorld,ByeWorld", result.get());
    }

    @Test
    void testShortListSyntax() {

        AtomicReference<List<String>> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t Repeat 3 " +
                "-t StringDef hello " +
                "-s greeting " +
                "-t Merge 'greeting > greetings[]' " +
                "-f greetings")
                .add(result::set)
                .run();

        assertEquals(result.get(), List.of("hello", "hello", "hello"));
    }

    @Test
    void testOptionalList() {

        AtomicReference<List<String>> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t Repeat 3 " +
                "-t StringDef hello " +
                "-s greeting " +
                "-t Merge 'notStashed > greetings[]?' " +
                "-f greetings")
                .add(result::set)
                .run();

        assertEquals(result.get(), List.of());
    }

    @Test
    void testNonOptionalListThrowsOnEmpty() {

        AtomicReference<IllegalStateException> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-e ExceptionHandler " +
                "-t Repeat 3 " +
                "-t StringDef hello " +
                "-s greeting " +
                "-t Merge 'notStashed > greetings[]' " +
                "-- " +
                "-t ExceptionHandler:Passthrough")
                .add(result::set)
                .run();

        assertEquals("No values for \"notStashed > greetings[]\"", result.get().getMessage());
    }

    @Test
    void testSameObjectDoesNotTriggerMultipleValues() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef hello " +
                "-t Repeat 3 " +
                "-s greeting " +
                "-t Merge 'greeting' " +
                "-f greeting")
                .add(result::set)
                .run();

        assertEquals("hello", result.get());
    }

    @Test
    void testBridgingAfterMerge() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef hello " +
                "-s " +
                "-t Merge stash " +
                "-f " +
                "-t BytesToString")
                .add(result::set)
                .run();

        assertEquals("hello", result.get());
    }

    @Test
    void testTreeSet() {

        AtomicReference<Set<String>> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t Repeat 3 " +
                "-t Cycle B B A " +
                "-s letter " +
                "-t Merge 'letter > letters{tree}' " +
                "-f letters")
                .add(result::set)
                .run();

        Iterator<String> iterator = result.get().iterator();
        assertEquals("A", iterator.next());
        assertEquals("B", iterator.next());
        assertFalse(iterator.hasNext());
    }
}
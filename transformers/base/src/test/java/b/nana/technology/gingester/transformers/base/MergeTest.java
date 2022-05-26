package b.nana.technology.gingester.transformers.base;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MergeTest {

    @Test
    void testBasic() {

        Gingester gingester = new Gingester().cli("" +
                "-t Passthrough " +
                "-l A B " +
                "-t A:StringCreate HelloWorld -s hello -l Merge " +
                "-t B:StringCreate ByeWorld -s bye -l Merge " +
                "-t Merge \"['hello', 'bye']\" " +
                "-t StringCreate '${hello},${bye}'");

        AtomicReference<String> result = new AtomicReference<>();
        gingester.attach(result::set);

        gingester.run();

        assertEquals("HelloWorld,ByeWorld", result.get());
    }

    @Test
    void testList() {

        Gingester gingester = new Gingester().cli("" +
                "-t Passthrough " +
                "-l A B " +
                "-t A:StringCreate HelloWorld -s message -l Merge " +
                "-t B:StringCreate ByeWorld -s message -l Merge " +
                "-t Merge [{fetch:'message',stash:'messages',list:true}] " +
                "-f messages " +
                "-t ListStream " +
                "-t StringToInputStream " +
                "-t InputStreamJoin , " +
                "-t InputStreamToString");

        AtomicReference<String> result = new AtomicReference<>();
        gingester.attach(result::set);

        gingester.run();

        assertEquals("HelloWorld,ByeWorld", result.get());
    }

    @Test
    void testShortListSyntax() {

        AtomicReference<List<String>> result = new AtomicReference<>();

        new Gingester().cli("" +
                "-t Repeat 3 " +
                "-t StringCreate hello " +
                "-s greeting " +
                "-t Merge 'greeting > greetings[]' " +
                "-f greetings")
                .attach(result::set)
                .run();

        assertEquals(result.get(), List.of("hello", "hello", "hello"));
    }

    @Test
    void testOptionalList() {

        AtomicReference<List<String>> result = new AtomicReference<>();

        new Gingester().cli("" +
                "-t Repeat 3 " +
                "-t StringCreate hello " +
                "-s greeting " +
                "-t Merge 'notStashed > greetings[]?' " +
                "-f greetings")
                .attach(result::set)
                .run();

        assertEquals(result.get(), List.of());
    }

    @Test
    void testNonOptionalListThrowsOnEmpty() {

        AtomicReference<IllegalStateException> result = new AtomicReference<>();

        new Gingester().cli("" +
                "-e ExceptionHandler " +
                "-t Repeat 3 " +
                "-t StringCreate hello " +
                "-s greeting " +
                "-t Merge 'notStashed > greetings[]' " +
                "-- " +
                "-t ExceptionHandler:Passthrough")
                .attach(result::set)
                .run();

        assertEquals("No values for \"notStashed > greetings[]\"", result.get().getMessage());
    }

    @Test
    void testSameObjectDoesNotTriggerMultipleValues() {

        AtomicReference<String> result = new AtomicReference<>();

        new Gingester().cli("" +
                "-t StringCreate hello " +
                "-t Repeat 3 " +
                "-s greeting " +
                "-t Merge 'greeting' " +
                "-f greeting")
                .attach(result::set)
                .run();

        assertEquals("hello", result.get());
    }
}
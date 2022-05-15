package b.nana.technology.gingester.transformers.base;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

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
}
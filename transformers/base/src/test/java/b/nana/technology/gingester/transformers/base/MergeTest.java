package b.nana.technology.gingester.transformers.base;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MergeTest {

    @Test
    void test() {

        Gingester gingester = new Gingester("" +
                "-t Passthrough " +
                "-l A B " +
                "-t A:StringCreate HelloWorld -s message -l Merge " +
                "-t B:StringCreate ByeWorld -s message -l Merge " +
                "-t Merge [{fetch:'message',stash:'messages'}] " +
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
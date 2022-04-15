package b.nana.technology.gingester.transformers.base.transformers.groupby;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EqualsTest {

    @Test
    void test() {

        Gingester gingester = new Gingester("" +
                "-t Repeat 25 " +
                "-t StringCreate 'Hello, World ${description?string.computer[0..0]}!' " +
                "-sft GroupByEquals " +
                "-stt InputStreamJoin " +
                "-t InputStreamToString");

        ArrayDeque<String> result = new ArrayDeque<>();
        gingester.attach(result::add);

        gingester.run();

        assertEquals(10, result.size());
        assertEquals(25, result.stream().mapToInt(s -> s.split("!").length).sum());
    }

    @Test
    void testPureBridgeAfterFetchGroupKey() {

        AtomicReference<String> result = new AtomicReference<>();

        new Gingester("" +
                "-t StringCreate 'Hello, World!' " +
                "-sft GroupByEquals customKey " +  // customKey is type String
                "-stt InputStreamJoin " +
                "-t InputStreamDrain " +
                "-f customKey " +
                "-t BytesToString")  // Pure StringToBytes must be inserted
                .attach(result::set)
                .run();

        assertEquals("Hello, World!", result.get());
    }
}
package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PeekBytesTest {

    @Test
    void test() {

        AtomicReference<String> peekResult = new AtomicReference<>();
        AtomicReference<String> fullResult = new AtomicReference<>();

        new Gingester().cli("" +
                "-t StringCreate 'Hello, World!' " +
                "-t PeekBytes 5 " +
                "-t BytesToString " +
                "-f " +
                "-t InputStreamToString")
                .attach(peekResult::set, "BytesToString")
                .attach(fullResult::set)
                .run();

        assertEquals("Hello", peekResult.get());
        assertEquals("Hello, World!", fullResult.get());
    }
}
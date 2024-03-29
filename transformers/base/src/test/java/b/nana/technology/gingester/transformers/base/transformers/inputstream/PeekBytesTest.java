package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PeekBytesTest {

    @Test
    void test() {

        AtomicReference<String> peekResult = new AtomicReference<>();
        AtomicReference<String> fullResult = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t PeekBytes 5 " +
                "-t BytesToString " +
                "-f " +
                "-t InputStreamToString")
                .addTo(peekResult::set, "BytesToString")
                .addTo(fullResult::set, "InputStreamToString")
                .run();

        assertEquals("Hello", peekResult.get());
        assertEquals("Hello, World!", fullResult.get());
    }

    @Test
    void testPeekALot() {

        AtomicReference<String> peekResult = new AtomicReference<>();
        AtomicReference<String> fullResult = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t PeekBytes 500 " +
                "-t BytesToString " +
                "-f " +
                "-t InputStreamToString")
                .addTo(peekResult::set, "BytesToString")
                .addTo(fullResult::set, "InputStreamToString")
                .run();

        assertEquals("Hello, World!", peekResult.get());
        assertEquals("Hello, World!", fullResult.get());
    }
}
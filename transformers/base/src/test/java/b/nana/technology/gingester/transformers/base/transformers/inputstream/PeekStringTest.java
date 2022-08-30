package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PeekStringTest {

    @Test
    void test() {

        AtomicReference<String> peekResult = new AtomicReference<>();
        AtomicReference<String> fullResult = new AtomicReference<>();

        new FlowBuilder().cli("" +
                        "-t StringDef 'Hello, World!' " +
                        "-t PeekString 500 " +
                        "-f " +
                        "-t InputStreamToString")
                .addTo(peekResult::set, "PeekString")
                .addTo(fullResult::set, "InputStreamToString")
                .run();

        assertEquals("Hello, World!", peekResult.get());
        assertEquals("Hello, World!", fullResult.get());
    }

}
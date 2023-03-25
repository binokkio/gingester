package b.nana.technology.gingester.transformers.tika;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TikaTest {

    @Test
    void testSimple() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t Tika " +
                "-t InputStreamToString")
                .add(result::set)
                .run();

        assertEquals("Hello, World!\n", result.get());
    }
}
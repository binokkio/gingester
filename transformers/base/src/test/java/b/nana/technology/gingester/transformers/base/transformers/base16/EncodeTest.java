package b.nana.technology.gingester.transformers.base.transformers.base16;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EncodeTest {

    @Test
    void test() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t Base16Encode")
                .add(result::set)
                .run();

        assertEquals("48656c6c6f2c20576f726c6421", result.get());
    }
}
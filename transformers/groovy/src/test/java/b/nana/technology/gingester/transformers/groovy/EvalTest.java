package b.nana.technology.gingester.transformers.groovy;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class EvalTest {

    @Test
    void test() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef Hello " +
                "-t Eval 'in + \", World!\"'")
                .add(result::set)
                .run();

        assertEquals("Hello, World!", result.get());
    }
}

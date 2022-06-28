package b.nana.technology.gingester.transformers.groovy;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class EvalTest {

    @Test
    void test() {

        AtomicReference<String> result = new AtomicReference<>();

        new Gingester().cli("" +
                "-t StringDef Hello " +
                "-t Eval 'in + \", World!\"'")
                .attach(result::set)
                .run();

        assertEquals("Hello, World!", result.get());
    }
}

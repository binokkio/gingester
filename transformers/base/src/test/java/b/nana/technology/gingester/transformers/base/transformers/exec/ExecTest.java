package b.nana.technology.gingester.transformers.base.transformers.exec;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ExecTest {

    @Test
    void test() {

        Gingester gingester = new Gingester().cli("" +
                "-t StringCreate 'Hello, World!' " +
                "-t Exec '/bin/cat' " +
                "-t InputStreamToString");

        AtomicReference<String> result = new AtomicReference<>();
        gingester.attach(result::set);

        gingester.run();

        assertEquals("Hello, World!", result.get());
    }
}
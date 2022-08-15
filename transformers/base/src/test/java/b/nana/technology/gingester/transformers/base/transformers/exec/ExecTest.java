package b.nana.technology.gingester.transformers.base.transformers.exec;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ExecTest {

    @Test
    void test() {

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-t StringDef 'Hello, World!' " +
                "-t Exec '/bin/cat' " +
                "-t InputStreamToString");

        AtomicReference<String> result = new AtomicReference<>();
        flowBuilder.add(result::set);

        flowBuilder.run();

        assertEquals("Hello, World!", result.get());
    }
}
package b.nana.technology.gingester;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TemplatingTest {

    @Test
    void test() {
        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t Repeat 3 " +
                "-t Cycle A B C " +
                "-s " +
                "-f description " +
                "-t MapCollect " +
                "-s map " +
                "-t StringDef '<#list map as count, value>${count}: ${value}, </#list>'")
                .add(result::set)
                .run();

        assertEquals("0: A, 1: B, 2: C, ", result.get());
    }
}

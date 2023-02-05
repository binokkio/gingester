package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GcliTest {

    @Test
    void test() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-ss target World " +
                "-t Gcli '\"-t StringDef \\'Hello, ${target}!\\'\"'")
                .add(result::set)
                .run();

        assertEquals("Hello, World!", result.get());
    }
}
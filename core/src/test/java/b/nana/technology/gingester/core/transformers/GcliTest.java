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

    @Test
    void testWithKwargs() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                        "-ss target World " +
                        "-t Gcli @ << EOG -t StringDef 'Hello, ${target} ${kwarg}!' EOG '{kwarg: 123}'")
                .add(result::set)
                .run();

        assertEquals("Hello, World 123!", result.get());
    }

    @Test
    void testWithOneSourceObject() {

        AtomicReference<String> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                        "-t StringDef 'Hello, World!' " +
                        "-t Gcli '{source: \"-pt\"}'")
                .add(result::set)
                .run();

        assertEquals("Hello, World!", result.get());
    }
}
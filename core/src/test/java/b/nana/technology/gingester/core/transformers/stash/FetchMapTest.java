package b.nana.technology.gingester.core.transformers.stash;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FetchMapTest {

    @Test
    void test() {

        AtomicReference<Map<String, String>> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-ss hello hello " +
                "-ss world world " +
                "-t FetchMap hello world")
                .add(result::set)
                .run();

        assertEquals(2, result.get().size());
        assertEquals("hello", result.get().get("hello"));
        assertEquals("world", result.get().get("world"));
    }

    @Test
    void testAs() {

        AtomicReference<Map<String, String>> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t StringDef hello -s " +
                "-t StringDef world -s " +
                "-t FetchMap '^2 > greeting' '^1 > target'")
                .add(result::set)
                .run();

        assertEquals(2, result.get().size());
        assertEquals("hello", result.get().get("greeting"));
        assertEquals("world", result.get().get("target"));
    }
}
package b.nana.technology.gingester.transformers.base.transformers.map;

import b.nana.technology.gingester.core.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StreamTest {

    @Test
    void test() {

        Set<String> results = new HashSet<>();

        new FlowBuilder().cli("" +
                "-t Repeat 3 " +
                "-t StringDef 'Hello, World ${description}!' " +
                "-s -f description " +
                "-t MapCollect " +
                "-t MapStream")
                .add(results::add)
                .run();

        assertEquals(3, results.size());
        assertTrue(results.contains("Hello, World 0!"));
        assertTrue(results.contains("Hello, World 1!"));
        assertTrue(results.contains("Hello, World 2!"));
    }
}

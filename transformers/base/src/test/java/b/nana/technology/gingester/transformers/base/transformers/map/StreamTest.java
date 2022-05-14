package b.nana.technology.gingester.transformers.base.transformers.map;

import b.nana.technology.gingester.core.Gingester;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamTest {

    @Test
    void test() {

        Set<String> results = new HashSet<>();

        new Gingester().cli("" +
                "-t Repeat 3 " +
                "-t StringCreate 'Hello, World ${description}!' " +
                "-s -f description " +
                "-t MapCollect " +
                "-t MapStream")
                .attach(results::add)
                .run();

        assertEquals(3, results.size());
        assertTrue(results.contains("Hello, World 0!"));
        assertTrue(results.contains("Hello, World 1!"));
        assertTrue(results.contains("Hello, World 2!"));
    }
}

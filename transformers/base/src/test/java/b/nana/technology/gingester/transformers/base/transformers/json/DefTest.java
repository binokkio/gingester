package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Gingester;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.*;

class DefTest {

    @Test
    void test() {

        Deque<JsonNode> results = new ArrayDeque<>();

        new Gingester().cli("" +
                "-t Repeat 2 " +
                "-t JsonDef '[]' " +
                "-s " +
                "-t StringDef 'Hello, World ${description}!' " +
                "-t StringAsJsonNode " +
                "-t JsonAdd")
                .attach(results::add)
                .run();

        assertEquals(2, results.size());
        assertEquals(1, results.getFirst().size());
        assertEquals("Hello, World 0!", results.getFirst().get(0).asText());
        assertEquals(1, results.getLast().size());
        assertEquals("Hello, World 1!", results.getLast().get(0).asText());
    }
}
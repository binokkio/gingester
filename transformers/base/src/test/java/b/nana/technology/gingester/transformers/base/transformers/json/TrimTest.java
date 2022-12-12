package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.FlowBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TrimTest {

    @Test
    void testNoArgs() {

        AtomicReference<JsonNode> result = new AtomicReference<>();

        new FlowBuilder()
                .cli("" +
                        "-t ResourceOpen '/data/json/trim-test.json' " +
                        "-t JsonTrim")
                .add(result::set)
                .run();

        assertEquals(1, result.get().size());
        assertEquals("value", result.get().get("normal").asText());
    }

    @Test
    void testArgs() {

        AtomicReference<JsonNode> result = new AtomicReference<>();

        new FlowBuilder()
                .cli("" +
                        "-t ResourceOpen '/data/json/trim-test.json' " +
                        "-t JsonTrim !emptyArrays")
                .add(result::set)
                .run();

        assertEquals(5, result.get().size());
        assertEquals("value", result.get().get("normal").asText());
        assertEquals("[]", result.get().get("empty-array").toString());
        assertEquals("[[]]", result.get().get("array").toString());
        assertEquals("{\"a\":[]}", result.get().get("object").toString());
        assertEquals("[{\"a\":[]}]", result.get().get("deep").toString());
    }

    @Test
    void testTrimToNull() {

        AtomicReference<JsonNode> result = new AtomicReference<>();

        new FlowBuilder()
                .cli("" +
                        "-t ResourceOpen '/data/json/trim-test.json' " +
                        "-t JsonTrim !nulls")
                .add(result::set)
                .run();

        assertTrue(result.get().get("whitespace").isNull());
    }
}
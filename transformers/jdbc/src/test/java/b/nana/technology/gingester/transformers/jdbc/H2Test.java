package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.FlowBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class H2Test {

    private static final String H2_URL = "jdbc:h2:mem:yield-generated-keys;DATABASE_TO_UPPER=false";
    private static final String AUTO_INCREMENT_KEYWORD = "AUTO_INCREMENT";

    @Test
    void testYieldGeneratedKeys() {

        ArrayDeque<Map<String, Integer>> result = new ArrayDeque<>();

        new FlowBuilder()
                .cli(getClass().getResource("/repeated_insert.cli"), Map.of(
                        "url", H2_URL,
                        "autoIncrementKeyword", AUTO_INCREMENT_KEYWORD
                ))
                .add(result::add)
                .run();

        assertEquals(1, result.remove().get("id"));
        assertEquals(2, result.remove().get("id"));
        assertEquals(3, result.remove().get("id"));
    }

    @Test
    void testYieldGeneratedKeysCanBeDisabled() {

        ArrayDeque<JsonNode> result = new ArrayDeque<>();

        new FlowBuilder()
                .cli(getClass().getResource("/repeated_insert.cli"), Map.of(
                        "url", H2_URL,
                        "autoIncrementKeyword", AUTO_INCREMENT_KEYWORD,
                        "yieldGeneratedKeys", false
                ))
                .add(result::add)
                .run();

        assertEquals(123, result.remove().get("a").intValue());
        assertEquals("Hello, World!", result.remove().get("b").textValue());
        assertTrue(result.remove().get("c").booleanValue());
    }
}

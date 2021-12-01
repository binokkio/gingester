package b.nana.technology.gingester.core.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContextTest {

    @Test
    void testFetchJsonNodeTraversal() {

        ObjectNode stash = JsonNodeFactory.instance.objectNode();

        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        stash.set("array", array);

        array.add("Hello, World!");

        Context context = new Context.Builder()
                .stash("stash", stash)
                .build();

        String result = ((JsonNode) context.fetch("stash", "array", "0").findFirst().orElseThrow()).asText();
        assertEquals("Hello, World!", result);
    }
}
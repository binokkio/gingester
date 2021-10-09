package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.UniReceiver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoveTest {

    @Test
    void testRemove() {

        AtomicReference<JsonNode> result = new AtomicReference<>();

        ObjectNode in = JsonNodeFactory.instance.objectNode();
        in.put("hello", "world");
        in.put("bye", "world");

        new Remove(new Remove.Parameters("$.bye")).transform(
                new Context.Builder().build(),
                in,
                (UniReceiver<JsonNode>) result::set
        );

        assertTrue(result.get().has("hello"));
        assertFalse(result.get().has("bye"));
    }

    @Test
    void testRemoveNested() {

        AtomicReference<JsonNode> result = new AtomicReference<>();

        ObjectNode in = JsonNodeFactory.instance.objectNode();
        in.put("hello", "world");
        in.put("bye", "world");

        ObjectNode nested = JsonNodeFactory.instance.objectNode();
        in.set("nested", nested);
        nested.put("foo", 123);
        nested.put("bar", 234);

        new Remove(new Remove.Parameters("$.nested.bar")).transform(
                new Context.Builder().build(),
                in,
                (UniReceiver<JsonNode>) result::set
        );

        assertTrue(result.get().has("hello"));
        assertTrue(result.get().has("bye"));
        assertTrue(result.get().has("nested"));
        assertTrue(result.get().get("nested").has("foo"));
        assertFalse(result.get().get("nested").has("bar"));
    }
}
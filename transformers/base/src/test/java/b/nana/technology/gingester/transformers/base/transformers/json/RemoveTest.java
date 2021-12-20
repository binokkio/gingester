package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.UniReceiver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RemoveTest {

    @Test
    void testJsonRemoveOutputsRemovedJsonNode() {

        Gingester gingester = new Gingester();
        gingester.cli("" +
                "-t JsonCreate \"{hello:'world a',bye:'world b'}\" " +
                "-t JsonRemove $.bye");

        AtomicReference<JsonNode> result = new AtomicReference<>();
        gingester.add(result::set);
        gingester.run();

        assertEquals("world b", result.get().textValue());
    }

    @Test
    void testRemoveModifiesStashedInput() {

        Gingester gingester = new Gingester();
        gingester.cli("" +
                "-t JsonCreate {hello:'world',bye:'world'} " +
                "-s -t JsonRemove $.bye -f");

        AtomicReference<JsonNode> result = new AtomicReference<>();
        gingester.add(result::set);
        gingester.run();

        assertTrue(result.get().has("hello"));
        assertFalse(result.get().has("bye"));
    }

    @Test
    void testNested() {

        Gingester gingester = new Gingester();
        gingester.cli("" +
                "-t JsonCreate \"{hello:'world',bye:'world',nested:{foo:123,bar:234}}\" " +
                "-s -t JsonRemove $.nested.bar -f");

        AtomicReference<JsonNode> result = new AtomicReference<>();
        gingester.add(result::set);
        gingester.run();

        assertTrue(result.get().has("hello"));
        assertTrue(result.get().has("bye"));
        assertTrue(result.get().has("nested"));
        assertTrue(result.get().get("nested").has("foo"));
        assertFalse(result.get().get("nested").has("bar"));
    }
}
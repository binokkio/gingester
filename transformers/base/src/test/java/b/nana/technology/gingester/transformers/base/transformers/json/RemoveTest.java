package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.core.transformers.passthrough.ConsumerPassthrough;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RemoveTest {

    @Test
    void testJsonRemoveOutputsRemovedJsonNode() {

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-t JsonDef \"{hello:'world a',bye:'world b'}\" " +
                "-t JsonRemove $.bye");

        AtomicReference<JsonNode> result = new AtomicReference<>();
        flowBuilder.add(result::set);
        flowBuilder.run();

        assertEquals("world b", result.get().textValue());
    }

    @Test
    void testRemoveModifiesStashedInput() {

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-t JsonDef {hello:'world',bye:'world'} " +
                "-s -t JsonRemove $.bye -f");

        AtomicReference<JsonNode> result = new AtomicReference<>();
        flowBuilder.add(result::set);
        flowBuilder.run();

        assertTrue(result.get().has("hello"));
        assertFalse(result.get().has("bye"));
    }

    @Test
    void testNested() {

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
                "-t JsonDef \"{hello:'world',bye:'world',nested:{foo:123,bar:234}}\" " +
                "-s -t JsonRemove $.nested.bar -f");

        AtomicReference<JsonNode> result = new AtomicReference<>();
        flowBuilder.add(result::set);
        flowBuilder.run();

        assertTrue(result.get().has("hello"));
        assertTrue(result.get().has("bye"));
        assertTrue(result.get().has("nested"));
        assertTrue(result.get().get("nested").has("foo"));
        assertFalse(result.get().get("nested").has("bar"));
    }

    @Test
    void testRequiredRemoveThrows() {

        AtomicReference<Exception> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-e ExceptionHandler " +
                "-t JsonDef \"{hello:'world'}\" " +
                "-t JsonRemove $.bye --")
                .node().id("ExceptionHandler").transformer(new ConsumerPassthrough<>(result::set)).add()
                .run();

        assertEquals(NoSuchElementException.class, result.get().getClass());
        assertEquals("$.bye", result.get().getMessage());
    }

    @Test
    void testOptionalRemoveDoesNotThrow() {

        AtomicReference<Exception> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-e ExceptionHandler " +
                "-t JsonDef \"{hello:'world'}\" " +
                "-t JsonRemove $.bye optional --")
                .node().id("ExceptionHandler").transformer(new ConsumerPassthrough<>(result::set)).add()
                .run();

        assertNull(result.get());
    }

    @Test
    void testRemoveEmptyIndefiniteDoesNotThrow() {

        AtomicReference<Exception> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-e ExceptionHandler " +
                "-t JsonDef \"{hello:'world'}\" " +
                "-t JsonRemove $..bye optional --")
                .node().id("ExceptionHandler").transformer(new ConsumerPassthrough<>(result::set)).add()
                .run();

        assertNull(result.get());
    }
}
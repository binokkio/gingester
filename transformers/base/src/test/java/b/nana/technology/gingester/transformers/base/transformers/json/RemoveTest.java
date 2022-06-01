package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Gingester;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RemoveTest {

    @Test
    void testJsonRemoveOutputsRemovedJsonNode() {

        Gingester gingester = new Gingester().cli("" +
                "-t JsonCreate \"{hello:'world a',bye:'world b'}\" " +
                "-t JsonRemove $.bye");

        AtomicReference<JsonNode> result = new AtomicReference<>();
        gingester.attach(result::set);
        gingester.run();

        assertEquals("world b", result.get().textValue());
    }

    @Test
    void testRemoveModifiesStashedInput() {

        Gingester gingester = new Gingester().cli("" +
                "-t JsonCreate {hello:'world',bye:'world'} " +
                "-s -t JsonRemove $.bye -f");

        AtomicReference<JsonNode> result = new AtomicReference<>();
        gingester.attach(result::set);
        gingester.run();

        assertTrue(result.get().has("hello"));
        assertFalse(result.get().has("bye"));
    }

    @Test
    void testNested() {

        Gingester gingester = new Gingester().cli("" +
                "-t JsonCreate \"{hello:'world',bye:'world',nested:{foo:123,bar:234}}\" " +
                "-s -t JsonRemove $.nested.bar -f");

        AtomicReference<JsonNode> result = new AtomicReference<>();
        gingester.attach(result::set);
        gingester.run();

        assertTrue(result.get().has("hello"));
        assertTrue(result.get().has("bye"));
        assertTrue(result.get().has("nested"));
        assertTrue(result.get().get("nested").has("foo"));
        assertFalse(result.get().get("nested").has("bar"));
    }

    @Test
    void testRequiredRemoveThrows() {

        AtomicReference<Exception> result = new AtomicReference<>();

        new Gingester().cli("" +
                "-e ExceptionHandler " +
                "-t JsonCreate \"{hello:'world'}\" " +
                "-t JsonRemove $.bye --")
                .add("ExceptionHandler", result::set)
                .run();

        assertEquals(NoSuchElementException.class, result.get().getClass());
        assertEquals("$.bye", result.get().getMessage());
    }

    @Test
    void testOptionalRemoveDoesNotThrow() {

        AtomicReference<Exception> result = new AtomicReference<>();

        new Gingester().cli("" +
                "-e ExceptionHandler " +
                "-t JsonCreate \"{hello:'world'}\" " +
                "-t JsonRemove $.bye optional --")
                .add("ExceptionHandler", result::set)
                .run();

        assertNull(result.get());
    }
}
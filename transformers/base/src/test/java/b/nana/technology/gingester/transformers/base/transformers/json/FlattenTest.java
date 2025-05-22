package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.FlowBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlattenTest {

    @Test
    void test() {

        AtomicReference<JsonNode> result = new AtomicReference<>();

        new FlowBuilder().cli("""
                -t ResourceOpen /data/json/array-wrapped-objects.json
                -t JsonFlatten
                """)
                .add(result::set)
                .run();

        assertEquals(123, result.get().get("array[0].id").intValue());
        assertEquals("Hello, World 1!", result.get().get("array[0].message").textValue());
        assertEquals(234, result.get().get("array[1].id").intValue());
        assertEquals("Hello, World 2!", result.get().get("array[1].message").textValue());
        assertEquals(345, result.get().get("array[2].id").intValue());
        assertEquals("Hello, World 3!", result.get().get("array[2].message").textValue());
    }

    @Test
    void testUsePointers() {

        AtomicReference<JsonNode> result = new AtomicReference<>();

        new FlowBuilder().cli("""
                -t ResourceOpen /data/json/array-wrapped-objects.json
                -t JsonFlatten '/'
                """)
                .add(result::set)
                .run();

        assertEquals(123, result.get().get("array/0/id").intValue());
        assertEquals("Hello, World 1!", result.get().get("array/0/message").textValue());
        assertEquals(234, result.get().get("array/1/id").intValue());
        assertEquals("Hello, World 2!", result.get().get("array/1/message").textValue());
        assertEquals(345, result.get().get("array/2/id").intValue());
        assertEquals("Hello, World 3!", result.get().get("array/2/message").textValue());
    }
}
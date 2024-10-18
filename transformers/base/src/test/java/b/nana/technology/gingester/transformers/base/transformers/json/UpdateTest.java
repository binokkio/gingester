package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.FlowBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UpdateTest {

    @Test
    void test() {

        AtomicReference<JsonNode> result = new AtomicReference<>();

        new FlowBuilder().cli("""
                        -t JsonDef @ '{hello:123,world:{foo:"bar"}}'
                        -t JsonUpdate @ '{bye:234,world:{bar:"foo"}}'
                        """)
                .add(result::set)
                .run();

        assertEquals(123, result.get().get("hello").intValue());
        assertEquals(234, result.get().get("bye").intValue());
        assertEquals("bar", result.get().get("world").get("foo").textValue());
        assertEquals("foo", result.get().get("world").get("bar").textValue());
    }
}
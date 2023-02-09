package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.FlowBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class WrapTest {

    @Test
    void testKeyTemplate() {

        AtomicReference<JsonNode> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                        "-ss key message " +
                        "-t JsonDef @ '{\"hello\":\"world\"}' " +
                        "-t JsonWrap '${key}'")
                .add(result::set)
                .run();

        assertEquals("world", result.get().get("message").get("hello").asText());
    }
}
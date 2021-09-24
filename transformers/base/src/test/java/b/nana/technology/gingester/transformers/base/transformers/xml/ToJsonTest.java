package b.nana.technology.gingester.transformers.base.transformers.xml;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.UniReceiver;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToJsonTest {

    @Test
    void test() throws Exception {

        AtomicReference<JsonNode> result = new AtomicReference<>();

        new ToJson().transform(
                new Context.Builder().build(),
                getClass().getResourceAsStream("/b/nana/technology/gingester/transformers/base/transformers/xml/simple.xml"),
                (UniReceiver<JsonNode>) result::set
        );

        assertEquals("Hello, World!", result.get().get("message").asText());
        assertEquals("Hello", result.get().get("list").get("item").get(0).asText());
        assertEquals("World", result.get().get("list").get("item").get(1).asText());
    }
}
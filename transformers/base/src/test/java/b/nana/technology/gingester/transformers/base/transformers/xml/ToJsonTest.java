package b.nana.technology.gingester.transformers.base.transformers.xml;

import b.nana.technology.gingester.core.FlowBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ToJsonTest {

    @Test
    void test() {

        AtomicReference<JsonNode> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t ResourceOpen /data/xml/simple.xml " +
                "-t XmlToJson")
                .add(result::set)
                .run();

        assertEquals("Hello, World!", result.get().get("message").asText());
        assertEquals("Hello", result.get().get("list").get("item").get(0).asText());
        assertEquals("World", result.get().get("list").get("item").get(1).asText());
    }

    @Test
    void testMaxAttributeSizeExceeded() {

        AtomicReference<Exception> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-e Exceptions " +
                "-t ResourceOpen /data/xml/with-attributes.xml " +
                "-t XmlToJson '{maxAttributeSize: \"2b\"}' " +
                "-- " +
                "-t Exceptions:Passthrough")
                .add(result::set)
                .run();

        assertTrue(result.get().getMessage().startsWith("Maximum attribute size limit (2) exceeded"));
    }
}
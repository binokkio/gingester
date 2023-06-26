package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.core.receiver.UniReceiver;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CamelCaseTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);

    @Test
    void testSimple() throws JsonProcessingException {

        JsonNode in = OBJECT_MAPPER.readTree("{hello_world:'i_am_a_value',i_am_a_key:'i_am_a_different_value'}");
        AtomicReference<JsonNode> result = new AtomicReference<>();
        new CamelCase().transform(null, in, (UniReceiver<JsonNode>) result::set);

        assertEquals(2, result.get().size());
        assertEquals("i_am_a_value", result.get().get("helloWorld").asText());
        assertEquals("i_am_a_different_value", result.get().get("iAmAKey").asText());
    }

    @Test
    void testComplex() throws JsonProcessingException {
        JsonNode in = OBJECT_MAPPER.readTree("{object_node:{array_node:[{hello_world:'i_am_a_value',i_am_a_key:'i_am_a_different_value'}]}}");
        AtomicReference<JsonNode> result = new AtomicReference<>();
        new CamelCase().transform(null, in, (UniReceiver<JsonNode>) result::set);
        String resultJson = OBJECT_MAPPER.writeValueAsString(result.get());
        assertEquals("{\"objectNode\":{\"arrayNode\":[{\"helloWorld\":\"i_am_a_value\",\"iAmAKey\":\"i_am_a_different_value\"}]}}", resultJson);
    }

    @Test
    void testSpaces() {

        AtomicReference<JsonNode> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t JsonDef @ '{\"Here are Spaces\": 123}' " +
                "-t JsonCamelCase")
                .add(result::set)
                .run();

        assertEquals(123, result.get().get("HereAreSpaces").intValue());
    }
}
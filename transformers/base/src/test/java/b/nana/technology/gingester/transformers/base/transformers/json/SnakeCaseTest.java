package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.receiver.UniReceiver;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SnakeCaseTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);

    @Test
    void testSimple() throws JsonProcessingException {

        JsonNode in = OBJECT_MAPPER.readTree("{helloWorld:'iAmAValue',iAmAKey:'iAmADifferentValue'}");
        AtomicReference<JsonNode> result = new AtomicReference<>();
        new SnakeCase().transform(null, in, (UniReceiver<JsonNode>) result::set);

        assertEquals(2, result.get().size());
        assertEquals("iAmAValue", result.get().get("hello_world").asText());
        assertEquals("iAmADifferentValue", result.get().get("i_am_a_key").asText());
    }

    @Test
    void testComplex() throws JsonProcessingException {
        JsonNode in = OBJECT_MAPPER.readTree("{objectNode:{arrayNode:[{helloWorld:'iAmAValue',iAmAKey:'iAmADifferentValue'}]}}");
        AtomicReference<JsonNode> result = new AtomicReference<>();
        new SnakeCase().transform(null, in, (UniReceiver<JsonNode>) result::set);
        String resultJson = OBJECT_MAPPER.writeValueAsString(result.get());
        assertEquals("{\"object_node\":{\"array_node\":[{\"hello_world\":\"iAmAValue\",\"i_am_a_key\":\"iAmADifferentValue\"}]}}", resultJson);
    }
}
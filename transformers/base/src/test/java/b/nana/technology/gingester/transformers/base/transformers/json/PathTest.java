package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.UniReceiver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PathTest {

    @Test
    void testJsonPath() throws IOException {

        Queue<JsonNode> results = new ArrayDeque<>();

        JsonNode jsonNode = new ObjectMapper().readTree(getClass().getResourceAsStream("/b/nana/technology/gingester/transformers/base/data/json/array-wrapped-objects.json"));

        Path path = new Path(new Path.Parameters("$..message"));
        path.transform(
                new Context.Builder().build(),
                jsonNode,
                (UniReceiver<JsonNode>) results::add
        );

        assertEquals(3, results.size());
        assertEquals("Hello, World 1!", results.remove().textValue());
        assertEquals("Hello, World 2!", results.remove().textValue());
        assertEquals("Hello, World 3!", results.remove().textValue());
    }
}
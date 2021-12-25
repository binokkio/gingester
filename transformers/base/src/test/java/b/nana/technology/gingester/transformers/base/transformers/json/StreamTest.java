package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.UniReceiver;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreamTest {

    @Test
    void testJsonStream() {

        Queue<JsonNode> results = new ArrayDeque<>();

        Stream stream = new Stream(new Stream.Parameters("$..message"));
        stream.transform(
                new Context.Builder().build(),
                getClass().getResourceAsStream("/data/json/array-wrapped-objects.json"),
                (UniReceiver<JsonNode>) results::add
        );

        assertEquals(3, results.size());
        assertEquals("Hello, World 1!", results.remove().textValue());
        assertEquals("Hello, World 2!", results.remove().textValue());
        assertEquals("Hello, World 3!", results.remove().textValue());
    }
}
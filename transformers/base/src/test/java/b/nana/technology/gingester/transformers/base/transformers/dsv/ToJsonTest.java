package b.nana.technology.gingester.transformers.base.transformers.dsv;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.UniReceiver;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToJsonTest {

    @Test
    void testPlainCsv() throws Exception {

        Queue<JsonNode> results = new ArrayDeque<>();

        ToJson toJson = new ToJson(new ToJson.Parameters());
        toJson.transform(
                new Context.Builder().build(),
                getClass().getResourceAsStream("/b/nana/technology/gingester/transformers/base/transformers/dsv/test.csv"),
                (UniReceiver<JsonNode>) results::add
        );

        assertEquals("1", results.remove().get("a").asText());
        assertEquals("2", results.remove().get("a").asText());
        assertEquals("3", results.remove().get("a").asText());
    }

    @Test
    void testDsv() throws Exception {

        Queue<JsonNode> results = new ArrayDeque<>();

        ToJson.Parameters parameters = new ToJson.Parameters();
        parameters.delimiter = '|';
        parameters.quote = '?';

        ToJson toJson = new ToJson(parameters);
        toJson.transform(
                new Context.Builder().build(),
                getClass().getResourceAsStream("/b/nana/technology/gingester/transformers/base/transformers/dsv/test.dsv"),
                (UniReceiver<JsonNode>) results::add
        );

        assertEquals("1", results.remove().get("a").asText());
        assertEquals("2", results.remove().get("a").asText());
        assertEquals("3", results.remove().get("a").asText());
    }

    @Test
    void testWithConfiguredColumnNames() throws Exception {

        Queue<JsonNode> results = new ArrayDeque<>();

        ToJson.Parameters parameters = new ToJson.Parameters();
        parameters.columnNames = List.of("x", "y", "z");

        ToJson toJson = new ToJson(parameters);
        toJson.transform(
                new Context.Builder().build(),
                getClass().getResourceAsStream("/b/nana/technology/gingester/transformers/base/transformers/dsv/test.csv"),
                (UniReceiver<JsonNode>) results::add
        );

        assertEquals("a", results.remove().get("x").asText());
        assertEquals("1", results.remove().get("x").asText());
        assertEquals("2", results.remove().get("x").asText());
        assertEquals("3", results.remove().get("x").asText());
    }
}

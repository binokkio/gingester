package b.nana.technology.gingester.transformers.base.transformers.dsv;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.UniReceiver;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

    @Test
    void testWithEscapeCharacter() throws Exception {

        Queue<JsonNode> results = new ArrayDeque<>();

        ToJson.Parameters parameters = new ToJson.Parameters();
        parameters.escape = '\\';

        ToJson toJson = new ToJson(parameters);
        toJson.transform(
                new Context.Builder().build(),
                getClass().getResourceAsStream("/b/nana/technology/gingester/transformers/base/transformers/dsv/with-escapes.csv"),
                (UniReceiver<JsonNode>) results::add
        );

        assertEquals("2\"two\"", results.remove().get("b").asText());
        assertEquals("3,three", results.remove().get("b").asText());
        assertEquals("\"four\"", results.remove().get("b").asText());
    }

    @Test
    void testWithExtras() throws Exception {

        Queue<JsonNode> results = new ArrayDeque<>();

        ToJson toJson = new ToJson(new ToJson.Parameters());
        toJson.transform(
                new Context.Builder().build(),
                getClass().getResourceAsStream("/b/nana/technology/gingester/transformers/base/transformers/dsv/with-extras.csv"),
                (UniReceiver<JsonNode>) results::add
        );

        assertFalse(results.remove().has("__extras__"));
        assertEquals("[\"5\"]", results.remove().get("__extras__").toString());
        assertEquals("[\"6\",\"7\"]", results.remove().get("__extras__").toString());
    }
}

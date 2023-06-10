package b.nana.technology.gingester.transformers.poi;

import b.nana.technology.gingester.core.FlowBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToCsvTest {

    @Test
    void testXlsToCsv() {

        Deque<String> results = new LinkedBlockingDeque<>();

        new FlowBuilder().cli(
                "-t ResourceOpen /data.xls " +
                "-t XlsToCsv " +
                "-t ToLines")
                .add(results::add)
                .run();

        assertLineResults(results);
    }

    @Test
    void testXlsxToCsv() {

        Deque<String> results = new LinkedBlockingDeque<>();

        new FlowBuilder().cli(
                "-t ResourceOpen /data.xlsx " +
                "-t XlsxToCsv " +
                "-t ToLines")
                .add(results::add)
                .run();

        assertLineResults(results);
    }

    private void assertLineResults(Deque<String> results) {

        assertEquals("\"foo\",\"bar\",\"baz\"", results.removeFirst());
        assertEquals("1,\"New", results.removeFirst());
        assertEquals("line\",3", results.removeFirst());
        assertEquals("2,\"Something \"\"quoted\"\"\",4", results.removeFirst());
        assertEquals("3,\"sep,\",5", results.removeFirst());

        assertEquals("\"bar\",\"baz\",\"quux\"", results.removeFirst());
        assertEquals("5,6,7", results.removeFirst());
        assertEquals("6,7,8", results.removeFirst());
        assertEquals("7,8,9", results.removeFirst());
    }

    @Test
    void testXlsToJson() {

        Deque<JsonNode> results = new LinkedBlockingDeque<>();

        new FlowBuilder().cli(
                "-t ResourceOpen /data.xls " +
                "-t XlsToCsv " +
                "-t DsvToJson " +
                "-s record " +
                "-f XlsToCsv.description " +
                "-t StringToJsonNode " +
                "-t JsonSet sheetName")
                .add(results::add)
                .run();

        assertJsonResults(results);
    }

    @Test
    void testXlsxToJson() {

        Deque<JsonNode> results = new LinkedBlockingDeque<>();

        new FlowBuilder().cli(
                "-t ResourceOpen /data.xlsx " +
                "-t XlsxToCsv " +
                "-t DsvToJson " +
                "-s record " +
                "-f XlsxToCsv.description " +
                "-t StringToJsonNode " +
                "-t JsonSet sheetName")
                .add(results::add)
                .run();

        assertJsonResults(results);
    }

    private void assertJsonResults(Deque<JsonNode> results) {

        JsonNode record = results.removeFirst();
        assertEquals("Sheet1", record.get("sheetName").asText());
        assertEquals("1", record.get("foo").asText());
        assertEquals("New\nline", record.get("bar").asText());
        assertEquals("3", record.get("baz").asText());

        record = results.removeFirst();
        assertEquals("Sheet1", record.get("sheetName").asText());
        assertEquals("2", record.get("foo").asText());
        assertEquals("Something \"quoted\"", record.get("bar").asText());
        assertEquals("4", record.get("baz").asText());

        record = results.removeFirst();
        assertEquals("Sheet1", record.get("sheetName").asText());
        assertEquals("3", record.get("foo").asText());
        assertEquals("sep,", record.get("bar").asText());
        assertEquals("5", record.get("baz").asText());

        record = results.removeFirst();
        assertEquals("Sheet, \"two\"", record.get("sheetName").asText());
        assertEquals("5", record.get("bar").asText());
        assertEquals("6", record.get("baz").asText());
        assertEquals("7", record.get("quux").asText());

        record = results.removeFirst();
        assertEquals("Sheet, \"two\"", record.get("sheetName").asText());
        assertEquals("6", record.get("bar").asText());
        assertEquals("7", record.get("baz").asText());
        assertEquals("8", record.get("quux").asText());

        record = results.removeFirst();
        assertEquals("Sheet, \"two\"", record.get("sheetName").asText());
        assertEquals("7", record.get("bar").asText());
        assertEquals("8", record.get("baz").asText());
        assertEquals("9", record.get("quux").asText());
    }
}
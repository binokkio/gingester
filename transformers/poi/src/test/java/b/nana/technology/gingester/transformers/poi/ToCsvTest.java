package b.nana.technology.gingester.transformers.poi;

import b.nana.technology.gingester.core.FlowBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToCsvTest {

    @Test
    void testXls() {

        Deque<JsonNode> results = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-t ResourceOpen /data.xls " +
                "-t XlsToCsv " +
                "-t DsvToJson " +
                "-s record " +
                "-f XlsToCsv.description " +
                "-t StringToJsonNode " +
                "-t JsonSet sheetName")
                .add(results::add)
                .run();

        assertResults(results);
    }

    @Test
    void testXlsx() {

        Deque<JsonNode> results = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-t ResourceOpen /data.xlsx " +
                "-t XlsxToCsv " +
                "-t DsvToJson " +
                "-s record " +
                "-f XlsxToCsv.description " +
                "-t StringToJsonNode " +
                "-t JsonSet sheetName")
                .add(results::add)
                .run();

        assertResults(results);
    }

    void assertResults(Deque<JsonNode> result) {

        JsonNode record = result.removeFirst();
        assertEquals("Sheet1", record.get("sheetName").asText());
        assertEquals("1", record.get("foo").asText());
        assertEquals("New\nline", record.get("bar").asText());
        assertEquals("3", record.get("baz").asText());

        record = result.removeFirst();
        assertEquals("Sheet1", record.get("sheetName").asText());
        assertEquals("2", record.get("foo").asText());
        assertEquals("Something \"quoted\"", record.get("bar").asText());
        assertEquals("4", record.get("baz").asText());

        record = result.removeFirst();
        assertEquals("Sheet1", record.get("sheetName").asText());
        assertEquals("3", record.get("foo").asText());
        assertEquals("sep,", record.get("bar").asText());
        assertEquals("5", record.get("baz").asText());

        record = result.removeFirst();
        assertEquals("Sheet, \"two\"", record.get("sheetName").asText());
        assertEquals("5", record.get("bar").asText());
        assertEquals("6", record.get("baz").asText());
        assertEquals("7", record.get("quux").asText());

        record = result.removeFirst();
        assertEquals("Sheet, \"two\"", record.get("sheetName").asText());
        assertEquals("6", record.get("bar").asText());
        assertEquals("7", record.get("baz").asText());
        assertEquals("8", record.get("quux").asText());

        record = result.removeFirst();
        assertEquals("Sheet, \"two\"", record.get("sheetName").asText());
        assertEquals("7", record.get("bar").asText());
        assertEquals("8", record.get("baz").asText());
        assertEquals("9", record.get("quux").asText());
    }
}
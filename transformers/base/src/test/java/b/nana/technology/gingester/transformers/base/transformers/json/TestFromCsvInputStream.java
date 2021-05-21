package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Gingester;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestFromCsvInputStream {

    @Test
    void test() {

        FromCsvInputStream fromCsvInputStream = new FromCsvInputStream();

        Queue<JsonNode> results = new LinkedBlockingQueue<>();

        Gingester gingester = new Gingester();
        gingester.seed(fromCsvInputStream, getClass().getResourceAsStream("/test.csv"));
        gingester.link(fromCsvInputStream, results::add);
        gingester.run();

        assertEquals("1", results.remove().get("a").asText());
        assertEquals("2", results.remove().get("a").asText());
        assertEquals("3", results.remove().get("a").asText());
    }
}

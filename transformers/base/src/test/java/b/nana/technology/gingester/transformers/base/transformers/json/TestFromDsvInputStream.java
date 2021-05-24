package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Configuration;
import b.nana.technology.gingester.core.Gingester;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestFromDsvInputStream {

    @Test
    void test() {

        FromDsvInputStream fromDsvInputStream = new FromDsvInputStream(new FromDsvInputStream.Parameters());

        Queue<JsonNode> results = new LinkedBlockingQueue<>();

        Gingester.Builder gBuilder = new Gingester.Builder();
        gBuilder.seed(fromDsvInputStream, getClass().getResourceAsStream("/test.csv"));
        gBuilder.link(fromDsvInputStream, results::add);
        gBuilder.build().run();

        assertEquals("1", results.remove().get("a").asText());
        assertEquals("2", results.remove().get("a").asText());
        assertEquals("3", results.remove().get("a").asText());
    }

    @Test
    void testWithConfiguredColumnNames() {

        FromDsvInputStream.Parameters parameters = new FromDsvInputStream.Parameters();
        parameters.columnNames = List.of("x", "y", "z");
        FromDsvInputStream fromDsvInputStream = new FromDsvInputStream(parameters);

        Queue<JsonNode> results = new LinkedBlockingQueue<>();

        Gingester.Builder gBuilder = new Gingester.Builder();
        gBuilder.seed(fromDsvInputStream, getClass().getResourceAsStream("/test.csv"));
        gBuilder.link(fromDsvInputStream, results::add);
        Gingester gingester = gBuilder.build();
        gingester.run();

        assertEquals("a", results.remove().get("x").asText());
        assertEquals("1", results.remove().get("x").asText());
        assertEquals("2", results.remove().get("x").asText());
        assertEquals("3", results.remove().get("x").asText());
    }

    @Test
    void testWithConfiguration() throws IOException {

        Configuration configuration = Configuration.fromJson(getClass().getResourceAsStream("/dsv.gingester.json"));
        Gingester.Builder gBuilder = configuration.toBuilder();

        Queue<JsonNode> results = new LinkedBlockingQueue<>();

        FromDsvInputStream fromDsvInputStream = gBuilder.getTransformer("Json.FromDsvInputStream", FromDsvInputStream.class);
        gBuilder.seed(fromDsvInputStream, getClass().getResourceAsStream("/test.dsv"));
        gBuilder.link(fromDsvInputStream, results::add);
        gBuilder.build().run();

        assertEquals("a", results.remove().get("x").asText());
        assertEquals("1", results.remove().get("x").asText());
        assertEquals("2", results.remove().get("x").asText());
        assertEquals("3", results.remove().get("x").asText());
    }
}

package b.nana.technology.gingester.transformers.statistics.transformers.json;

import b.nana.technology.gingester.core.Gingester;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestStatistics {

    @Test
    void test() {

        AtomicReference<JsonNode> result = new AtomicReference<>();

        Gingester gingester = new Gingester();

        gingester.configure(c -> c
                .transformer("Resource.Open")
                .parameters("/basic.csv")
                .syncs(List.of("Json.Statistics")));

        gingester.add("Dsv.ToJson");
        gingester.add("Json.Statistics");
        gingester.add(result::set);
        gingester.run();

        assertEquals(26, result.get().get("/index").get("presence").get("count").intValue());
        assertEquals(100, result.get().get("/index").get("presence").get("percentage").intValue());
        assertEquals(26, result.get().get("/index").get("frequency").get("distinct").intValue());
        assertEquals(100, result.get().get("/index").get("frequency").get("percentage").intValue());
        assertEquals(10, result.get().get("/index").get("frequency").get("head").size());
        assertEquals(26, result.get().get("/index").get("numerical").get("count").intValue());
        assertEquals(100, result.get().get("/index").get("numerical").get("percentage").intValue());
        assertEquals(351, result.get().get("/index").get("numerical").get("sum").intValue());
        assertEquals(1, result.get().get("/index").get("numerical").get("min").intValue());
        assertEquals(26, result.get().get("/index").get("numerical").get("max").intValue());
        assertEquals(13.5, result.get().get("/index").get("numerical").get("mean").doubleValue());
        assertEquals(58.5, result.get().get("/index").get("numerical").get("variance").doubleValue());
        assertEquals(7.648529270389178, result.get().get("/index").get("numerical").get("standardDeviation").doubleValue());
        assertEquals(1, result.get().get("/index").get("numerical").get("histograms").size());

        assertEquals(0, result.get().get("/letter").get("numerical").get("count").intValue());
        assertEquals(1, result.get().get("/letter").get("numerical").size());

        assertEquals(22, result.get().get("/popularity").get("frequency").get("distinct").intValue());
        assertEquals(84.61538461538461, result.get().get("/popularity").get("frequency").get("percentage").doubleValue());
        assertEquals(".15", result.get().get("/popularity").get("frequency").get("head").get(0).get("value").textValue());
        assertEquals(2, result.get().get("/popularity").get("frequency").get("head").get(0).get("count").intValue());
        assertEquals(7.6923076923076925, result.get().get("/popularity").get("frequency").get("head").get(0).get("percentage").doubleValue());
        assertEquals(100, result.get().get("/popularity").get("numerical").get("sum").intValue());

        assertEquals(3, result.get().get("/vowel").get("frequency").get("distinct").intValue());
        assertEquals(11.538461538461538, result.get().get("/vowel").get("frequency").get("percentage").doubleValue());
        assertEquals(3, result.get().get("/vowel").get("frequency").get("head").size());
        assertEquals("no", result.get().get("/vowel").get("frequency").get("head").get(0).get("value").textValue());
        assertEquals(20, result.get().get("/vowel").get("frequency").get("head").get(0).get("count").doubleValue());
        assertEquals(76.92307692307693, result.get().get("/vowel").get("frequency").get("head").get(0).get("percentage").doubleValue());
        assertEquals("yes", result.get().get("/vowel").get("frequency").get("head").get(1).get("value").textValue());
        assertEquals(5, result.get().get("/vowel").get("frequency").get("head").get(1).get("count").doubleValue());
        assertEquals(19.230769230769234, result.get().get("/vowel").get("frequency").get("head").get(1).get("percentage").doubleValue());
        assertEquals("depends", result.get().get("/vowel").get("frequency").get("head").get(2).get("value").textValue());
        assertEquals(1, result.get().get("/vowel").get("frequency").get("head").get(2).get("count").doubleValue());
        assertEquals(3.8461538461538463, result.get().get("/vowel").get("frequency").get("head").get(2).get("percentage").doubleValue());
    }

    @Test
    void testJson() {

        AtomicReference<JsonNode> result = new AtomicReference<>();

        Gingester gingester = new Gingester();

        gingester.configure(c -> c
                .transformer("Resource.Open")
                .parameters("/basic.ndjson")
                .syncs(List.of("Json.Statistics")));

        gingester.configure(c -> c
                .transformer("InputStream.Split")
                .parameters("\n"));

        gingester.add("InputStream.ToJson");
        gingester.add("Json.Statistics");
        gingester.add(result::set);
        gingester.run();
    }

    @Test
    void testNulls() {

        AtomicReference<JsonNode> result = new AtomicReference<>();

        Gingester gingester = new Gingester();

        gingester.configure(c -> c
                .transformer("Resource.Open")
                .parameters("/nulls.ndjson")
                .syncs(List.of("Json.Statistics")));

        gingester.configure(c -> c
                .transformer("InputStream.Split")
                .parameters("\n"));

        gingester.add("InputStream.ToJson");
        gingester.add("Json.Statistics");
        gingester.add(result::set);
        gingester.run();

        assertEquals(2, result.get().get("/foo").get("numerical").get("count").intValue());
        assertEquals(9, result.get().get("/foo").get("numerical").size());
        assertEquals(1, result.get().get("/bar").get("numerical").get("count").intValue());
        assertEquals(9, result.get().get("/bar").get("numerical").size());
        assertEquals(0, result.get().get("/baz").get("numerical").get("count").intValue());
        assertEquals(1, result.get().get("/baz").get("numerical").size());
    }
}

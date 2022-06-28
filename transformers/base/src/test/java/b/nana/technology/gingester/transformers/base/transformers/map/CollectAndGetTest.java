package b.nana.technology.gingester.transformers.base.transformers.map;

import b.nana.technology.gingester.core.Gingester;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CollectAndGetTest {

    @Test
    void test() {

        AtomicReference<Map<?, ?>> map = new AtomicReference<>();
        AtomicReference<String> result = new AtomicReference<>();

        new Gingester().cli("" +
                "-t Repeat 3 " +
                "-t StringDef 'Hello, World ${description}!' " +
                "-s " +
                "-f description " +
                "-t MapCollect " +
                "-s map " +
                "-t IntDef 1 " +
                "-t MapGet")
                .attach(map::set, "MapCollect")
                .attach(result::set)
                .run();

        assertEquals(3, map.get().size());
        assertEquals("Hello, World 1!", result.get());
    }

    @Test
    void testJson() {

        Queue<JsonNode> results = new ArrayDeque<>();

        Gingester gingester = new Gingester().cli("" +
                "-t ResourceOpen /data/json/array-wrapped-objects.json " +
                "-t JsonStream $.array[*] " +
                "-s " +
                "-t 3 JsonPath $.id " +
                "-t JsonAsLong " +
                "-t MapCollect " +
                "-s map " +
                "-t ResourceOpen /data/dsv/with-external-references.csv " +
                "-t DsvToJson " +
                "-s " +
                "-t JsonPath $.reference " +
                "-t JsonAsLong " +
                "-t MapGet map " +
                "-t JsonSet lookup");

        gingester.attach(results::add);

        gingester.run();

        assertEquals(3, results.size());
        assertEquals("{\"name\":\"foo\",\"reference\":\"123\",\"lookup\":{\"id\":123,\"message\":\"Hello, World 1!\"}}", results.remove().toString());
        assertEquals("{\"name\":\"bar\",\"reference\":\"345\",\"lookup\":{\"id\":345,\"message\":\"Hello, World 3!\"}}", results.remove().toString());
        assertEquals("{\"name\":\"baz\",\"reference\":\"234\",\"lookup\":{\"id\":234,\"message\":\"Hello, World 2!\"}}", results.remove().toString());
    }
}

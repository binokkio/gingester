package b.nana.technology.gingester.transformers.base.transformers.map;

import b.nana.technology.gingester.core.FlowBuilder;
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

        new FlowBuilder().cli("" +
                "-t Repeat 3 " +
                "-t StringDef 'Hello, World ${description}!' " +
                "-s " +
                "-f description " +
                "-t MapCollect " +
                "-s map " +
                "-t IntDef 1 " +
                "-t MapGet")
                .addTo(map::set, "MapCollect")
                .addTo(result::set, "MapGet")
                .run();

        assertEquals(3, map.get().size());
        assertEquals("Hello, World 1!", result.get());
    }

    @Test
    void testJson() {

        Queue<JsonNode> results = new ArrayDeque<>();

        FlowBuilder flowBuilder = new FlowBuilder().cli("" +
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

        flowBuilder.add(results::add);

        flowBuilder.run();

        assertEquals(3, results.size());
        assertEquals("{\"name\":\"foo\",\"reference\":\"123\",\"lookup\":{\"id\":123,\"message\":\"Hello, World 1!\"}}", results.remove().toString());
        assertEquals("{\"name\":\"bar\",\"reference\":\"345\",\"lookup\":{\"id\":345,\"message\":\"Hello, World 3!\"}}", results.remove().toString());
        assertEquals("{\"name\":\"baz\",\"reference\":\"234\",\"lookup\":{\"id\":234,\"message\":\"Hello, World 2!\"}}", results.remove().toString());
    }
}

package b.nana.technology.gingester.transformers.base.transformers.map;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.core.Node;
import b.nana.technology.gingester.core.transformers.passthrough.ConsumerPassthrough;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
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

    @Test
    void testMapCollectThrowsOnCollisionByDefault() {

        AtomicReference<Exception> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-e ExceptionHandler " +
                "-t Repeat 3 " +
                "-t Cycle A B " +
                "-s " +
                "-t MapCollect Repeat.description " +
                "--")
                .add(new Node()
                        .id("ExceptionHandler")
                        .transformer(new ConsumerPassthrough<>(result::set)))
                .run();

        assertEquals("Collision for key: A", result.get().getMessage());
    }

    @Test
    void testMapCollectDisableThrowOnCollision() {

        AtomicReference<Map<String, Integer>> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t Repeat 3 " +
                "-t Cycle A B " +
                "-s " +
                "-t MapCollect Repeat.description !throwOnCollision")
                .add(result::set)
                .run();

        assertEquals(2, result.get().size());
        assertEquals(2, result.get().get("A"));
        assertEquals(1, result.get().get("B"));
    }

    @Test
    void testMapCollectDoesNotThrowOnCollisionForEqualValues() {

        AtomicReference<Map<String, String>> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t Repeat 3 " +
                "-t Cycle A B " +
                "-s " +
                "-t MapCollect")
                .add(result::set)
                .run();

        assertEquals(2, result.get().size());
        assertEquals("A", result.get().get("A"));
        assertEquals("B", result.get().get("B"));
    }

    @Test
    void testTreeMapCollect() {

        AtomicReference<Map<String, String>> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t Repeat 3 " +
                "-t Cycle A B " +
                "-s " +
                "-t MapCollect ^ tree")
                .add(result::set)
                .run();

        assertTrue(result.get() instanceof TreeMap);
    }
}

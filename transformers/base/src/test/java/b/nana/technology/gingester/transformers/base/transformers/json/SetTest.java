package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.FlowBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SetTest {

    @Test
    void testDefaultTarget() {

        AtomicReference<JsonNode> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t JsonDef '{}' " +
                "-s record " +
                "-t JsonDef '{hello:123}' " +
                "-t JsonSet greeting " +
                "-t JsonDef '{world:234}' " +
                "-t JsonSet greeted")
                .add(result::set)
                .run();

        assertEquals(123, result.get().get("greeting").get("hello").asInt());
        assertEquals(234, result.get().get("greeted").get("world").asInt());
    }

    @Test
    void testExplicitTarget() {

        AtomicReference<JsonNode> resultA = new AtomicReference<>();
        AtomicReference<JsonNode> resultB = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t JsonDef '{}' " +
                "-s a " +
                "-t JsonDef '{}' " +
                "-s b " +
                "-t JsonDef '{hello:123}' " +
                "-t JsonSet greeting a " +
                "-t JsonDef '{world:234}' " +
                "-t JsonSet greeted b " +
                "-t FetchA:Fetch a " +
                "-t FetchB:Fetch b")
                .addTo(resultA::set, "FetchA")
                .addTo(resultB::set, "FetchB")
                .run();

        assertEquals(123, resultA.get().get("greeting").get("hello").asInt());
        assertEquals(234, resultB.get().get("greeted").get("world").asInt());
    }

    @Test
    void testKeyTemplate() {

        AtomicReference<JsonNode> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-ss key hello " +
                "-t JsonDef '{}' " +
                "-s result " +
                "-t JsonDef '\"world\"' " +
                "-t JsonSet '${key}'")
                .add(result::set)
                .run();

        assertEquals("world", result.get().get("hello").asText());
    }
}
package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Gingester;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SetTest {

    @Test
    void testDefaultTarget() {

        AtomicReference<JsonNode> result = new AtomicReference<>();

        new Gingester().cli("" +
                "-t JsonCreate '{}' " +
                "-s record " +
                "-t JsonCreate '{hello:123}' " +
                "-t JsonSet greeting " +
                "-t JsonCreate '{world:234}' " +
                "-t JsonSet greeted")
                .attach(result::set)
                .run();

        assertEquals(123, result.get().get("greeting").get("hello").asInt());
        assertEquals(234, result.get().get("greeted").get("world").asInt());
    }

    @Test
    void testExplicitTarget() {

        AtomicReference<JsonNode> resultA = new AtomicReference<>();
        AtomicReference<JsonNode> resultB = new AtomicReference<>();

        new Gingester().cli("" +
                "-t JsonCreate '{}' " +
                "-s a " +
                "-t JsonCreate '{}' " +
                "-s b " +
                "-t JsonCreate '{hello:123}' " +
                "-t JsonSet greeting a " +
                "-t JsonCreate '{world:234}' " +
                "-t JsonSet greeted b " +
                "-t FetchA:Fetch a " +
                "-t FetchB:Fetch b")
                .attach(resultA::set, "FetchA")
                .attach(resultB::set, "FetchB")
                .run();

        assertEquals(123, resultA.get().get("greeting").get("hello").asInt());
        assertEquals(234, resultB.get().get("greeted").get("world").asInt());
    }
}
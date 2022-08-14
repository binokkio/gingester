package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.FlowBuilder;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.UniReceiver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PathTest {

    @Test
    void testJsonPath() throws IOException, NoSuchMethodException {

        Queue<JsonNode> results = new ArrayDeque<>();

        JsonNode jsonNode = new ObjectMapper().readTree(getClass().getResourceAsStream("/data/json/array-wrapped-objects.json"));

        Path.Parameters pathParameters = new Path.Parameters();
        pathParameters.path = "$..message";
        Path path = new Path(pathParameters);
        path.transform(
                Context.newTestContext(),
                jsonNode,
                (UniReceiver<JsonNode>) results::add
        );

        assertEquals(3, results.size());
        assertEquals("Hello, World 1!", results.remove().textValue());
        assertEquals("Hello, World 2!", results.remove().textValue());
        assertEquals("Hello, World 3!", results.remove().textValue());
    }

    @Test
    void testMissingDefinitePathThrows() {

        AtomicReference<Exception> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-e Exceptions " +
                "-t JsonDef '{hello:\"world\"}' " +
                "-t JsonPath '$.missing.path.should.throw' " +
                "-- " +
                "-t Exceptions:Passthrough")
                .attach(result::set)
                .run();

        assertNotNull(result.get());
    }

    @Test
    void testMissingOptionalDefinitePathDoesNotThrow() {

        AtomicReference<Exception> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-e Exceptions " +
                "-t JsonDef '{hello:\"world\"}' " +
                "-t JsonPath '$.missing.path.should.not.throw' optional " +
                "-- " +
                "-t Exceptions:Passthrough")
                .attach(result::set)
                .run();

        assertNull(result.get());
    }

    @Test
    void testMissingIndefinitePathThrows() {

        AtomicReference<Exception> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-e Exceptions " +
                "-t JsonDef '{hello:\"world\"}' " +
                "-t JsonPath '$.**.missing.path.should.throw' " +
                "-- " +
                "-t Exceptions:Passthrough")
                .attach(result::set)
                .run();

        assertNotNull(result.get());
    }

    @Test
    void testMissingOptionalIndefinitePathDoesNotThrow() {

        AtomicReference<Exception> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-e Exceptions " +
                "-t JsonDef '{hello:\"world\"}' " +
                "-t JsonPath '$.**.missing.path.should.not.throw' optional " +
                "-- " +
                "-t Exceptions:Passthrough")
                .attach(result::set)
                .run();

        assertNull(result.get());
    }
}
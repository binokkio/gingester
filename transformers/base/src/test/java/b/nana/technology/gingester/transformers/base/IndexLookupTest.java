package b.nana.technology.gingester.transformers.base;

import b.nana.technology.gingester.core.Gingester;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IndexLookupTest {

    @Test
    void test() {

        Queue<JsonNode> results = new ArrayDeque<>();

        Gingester gingester = new Gingester();

        gingester.cli("" +
                "-t Resource.Open /b/nana/technology/gingester/transformers/base/transformers/json/array-wrapped-objects.json " +
                "-t Json.Stream $.array[*] " +
                "-s " +
                "-t Json.Path $.id " +
                "-t Json.AsLong " +
                "-t Index " +
                "-t Resource.Open /b/nana/technology/gingester/transformers/base/transformers/dsv/with-external-references.csv " +
                "-t Dsv.ToJson " +
                "-s " +
                "-t Json.Path $.reference " +
                "-t Json.AsLong " +
                "-t Lookup " +
                "-w " +
                "-t Json.Set lookup");

        gingester.add(results::add);

        gingester.run();

        assertEquals(3, results.size());
        assertEquals("{\"name\":\"foo\",\"reference\":\"123\",\"lookup\":{\"id\":123,\"message\":\"Hello, World 1!\"}}", results.remove().toString());
        assertEquals("{\"name\":\"bar\",\"reference\":\"345\",\"lookup\":{\"id\":345,\"message\":\"Hello, World 3!\"}}", results.remove().toString());
        assertEquals("{\"name\":\"baz\",\"reference\":\"234\",\"lookup\":{\"id\":234,\"message\":\"Hello, World 2!\"}}", results.remove().toString());
    }
}

package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.FlowBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.*;

class ToJsonTest {

    @Test
    void testMultiLineObjects() {

        ArrayDeque<JsonNode> results = new ArrayDeque<>();

        new FlowBuilder().cli("" +
                "-t ResourceOpen /data/json/multi-line-objects.jsons " +
                "-t InputStreamToJson")
                .attach(results::add)
                .run();

        assertEquals("[1,2,3,4]", results.remove().toString());
        assertEquals("{\"one\":1}", results.remove().toString());
        assertEquals("{\"two\":2}", results.remove().toString());
        assertEquals("{\"three\":3}", results.remove().toString());
        assertEquals("{\"four\":4}", results.remove().toString());
    }
}
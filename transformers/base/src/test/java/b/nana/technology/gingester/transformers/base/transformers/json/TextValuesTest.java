package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.FlowBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TextValuesTest {

    @Test
    void test() {

        AtomicReference<JsonNode> result = new AtomicReference<>();

        new FlowBuilder().cli("" +
                "-t ResourceOpen /data/json/array-wrapped-objects.json " +
                "-t JsonTextValues")
                .add(result::set)
                .run();

        assertEquals("123", result.get().get("array").get(0).get("id").textValue());
    }
}
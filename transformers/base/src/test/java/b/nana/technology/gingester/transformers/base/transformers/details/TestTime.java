package b.nana.technology.gingester.transformers.base.transformers.details;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.transformers.base.common.ToJsonBase;
import b.nana.technology.gingester.transformers.base.transformers.string.Generate;
import b.nana.technology.gingester.transformers.base.transformers.string.ToJson;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestTime {

    @Test
    void testTime() {

        AtomicReference<Context> context = new AtomicReference<>();
        AtomicReference<JsonNode> result = new AtomicReference<>();

        Generate generate = new Generate(new Generate.Parameters("{\"Hello\":\"World!\"}"));
        Time<String> time = new Time<>();
        ToJson toJson = new ToJson(new ToJsonBase.Parameters());

        Gingester.Builder gBuilder = Gingester.newBuilder();
        gBuilder.link(generate, time);
        gBuilder.link(time, toJson);
        gBuilder.link(toJson, (c, r) -> {
            context.set(c);
            result.set(r);
        });
        gBuilder.build().run();

        assertEquals("World!", result.get().get("Hello").asText());
        assertTrue(context.get().fetch("time", "year").isPresent());
    }
}

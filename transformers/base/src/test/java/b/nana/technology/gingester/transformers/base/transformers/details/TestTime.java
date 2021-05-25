package b.nana.technology.gingester.transformers.base.transformers.details;

import b.nana.technology.gingester.core.Configuration;
import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.transformers.base.common.ToJsonBase;
import b.nana.technology.gingester.transformers.base.transformers.string.Generate;
import b.nana.technology.gingester.transformers.base.transformers.string.ToJson;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TestTime {

    @Test
    void testTime() {

        AtomicReference<Context> context = new AtomicReference<>();
        AtomicReference<JsonNode> result = new AtomicReference<>();

        Generate generate = new Generate(new Generate.Parameters("{\"Hello\":\"World!\"}"));
        Time<String> time = new Time<>();
        ToJson toJson = new ToJson(new ToJsonBase.Parameters());

        Gingester.Builder gBuilder = new Gingester.Builder();
        gBuilder.link(generate, time);
        gBuilder.link(time, toJson);
        gBuilder.link(toJson, (c, r) -> {
            context.set(c);
            result.set(r);
        });
        gBuilder.build().run();

        assertEquals("World!", result.get().get("Hello").asText());
        assertTrue(context.get().getDetail("time", "year").isPresent());
    }

    @Test
    void testTimeFromConfiguration() throws IOException {

        Configuration configuration = Configuration.fromJson(getClass().getResourceAsStream("/test-time.gingester.json"));
        Gingester.Builder gBuilder = configuration.toBuilder();

        AtomicReference<Context> context = new AtomicReference<>();
        AtomicReference<JsonNode> result = new AtomicReference<>();

        ToJson toJson = gBuilder.getTransformer("String.ToJson", ToJson.class);
        gBuilder.link(toJson, (c, r) -> {
            context.set(c);
            result.set(r);
        });

        gBuilder.build().run();

        assertEquals("World!", result.get().get("Hello").asText());
        assertTrue(context.get().getDetail("time", "year").isPresent());
    }

    @Test
    void testTimeFromBadConfiguration() throws IOException {
        Configuration configuration = Configuration.fromJson(getClass().getResourceAsStream("/test-time-bad-passthrough.gingester.json"));
        IllegalStateException e = assertThrows(IllegalStateException.class, configuration::toBuilder);
        assertEquals("Can't link from Details.Time to Json.Wrap, java.lang.String can not be assigned to com.fasterxml.jackson.databind.JsonNode", e.getMessage());
    }
}
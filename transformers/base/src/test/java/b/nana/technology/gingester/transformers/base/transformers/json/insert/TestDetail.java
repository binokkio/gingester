package b.nana.technology.gingester.transformers.base.transformers.json.insert;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.transformers.base.common.ToJsonBase;
import b.nana.technology.gingester.transformers.base.transformers.details.Time;
import b.nana.technology.gingester.transformers.base.transformers.string.ToJson;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestDetail {

    @Test
    void test() {

        ToJson toJson = new ToJson(new ToJsonBase.Parameters());
        Time<JsonNode> time = new Time<>();
        Detail detail = new Detail(new Detail.Parameters("time"));

        AtomicReference<JsonNode> result = new AtomicReference<>();

        Gingester.Builder gBuilder = new Gingester.Builder();
        gBuilder.seed(toJson, "{\"hello\":\"world\"}");
        gBuilder.link(toJson, time);
        gBuilder.link(time, detail);
        gBuilder.link(detail, result::set);
        gBuilder.build().run();

        assertEquals("world", result.get().get("hello").asText());
        assertTrue(result.get().path("time").path("year").isIntegralNumber());
    }
}

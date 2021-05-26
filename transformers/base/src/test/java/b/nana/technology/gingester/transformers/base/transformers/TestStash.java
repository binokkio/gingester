package b.nana.technology.gingester.transformers.base.transformers;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.transformers.base.common.ToJsonBase;
import b.nana.technology.gingester.transformers.base.transformers.json.FromStash;
import b.nana.technology.gingester.transformers.base.transformers.string.ToJson;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestStash {

    @Test
    void testStash() {

        ToJson toJson = new ToJson(new ToJsonBase.Parameters());
        Stash stash = new Stash(new Stash.Parameters());
        FromStash fromStash = new FromStash(new Stash.Parameters());

        AtomicReference<JsonNode> result = new AtomicReference<>();

        Gingester.Builder gBuilder = new Gingester.Builder();
        gBuilder.seed(toJson, "{\"hello\":\"world\"}");
        gBuilder.link(toJson, stash);
        gBuilder.link(stash, fromStash);
        gBuilder.link(fromStash, result::set);
        gBuilder.build().run();

        assertEquals("world", result.get().get("hello").asText());
    }
}

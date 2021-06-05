package b.nana.technology.gingester.transformers.base.transformers;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.transformers.base.common.ToJsonBase;
import b.nana.technology.gingester.transformers.base.transformers.inputstream.Gunzip;
import b.nana.technology.gingester.transformers.base.transformers.inputstream.ToString;
import b.nana.technology.gingester.transformers.base.transformers.json.Wrap;
import b.nana.technology.gingester.transformers.base.transformers.json.insert.Context;
import b.nana.technology.gingester.transformers.base.transformers.string.ToJson;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestHelloWorldNdjsonGz {

    @Test
    void testHelloWorldNdJsonGz() {

        Gunzip gunzip = new Gunzip();

        ToString.Parameters toStringParameters = new ToString.Parameters();
        toStringParameters.delimiter = "\n";
        ToString toString = new ToString(toStringParameters);

        ToJson toJson = new ToJson(new ToJsonBase.Parameters());
        Wrap wrap = new Wrap();
        Context insertContext = new Context();

        Queue<JsonNode> results = new LinkedBlockingQueue<>();

        Gingester.Builder gBuilder = Gingester.newBuilder();
        gBuilder.seed(gunzip, getClass().getResourceAsStream("/hello-world.ndjson.gz"));
        gBuilder.link(gunzip, toString);
        gBuilder.link(toString, toJson);
        gBuilder.link(toJson, wrap);
        gBuilder.link(wrap, insertContext);
        gBuilder.link(insertContext, results::add);
        gBuilder.build().run();

        Set<String> contexts = results.stream().map(jsonNode -> jsonNode.get("context").asText()).collect(Collectors.toSet());
        Set<String> messages = results.stream().map(jsonNode -> jsonNode.get("content").get("message").asText()).collect(Collectors.toSet());

        assertEquals(3, results.size());
        assertEquals(Set.of("1", "2", "3"), contexts);
        assertEquals(Set.of("Hello, World 1!", "Hello, World 2!", "Hello, World 3!"), messages);
    }
}

package b.nana.technology.gingester.transformers.base.transformers;

import b.nana.technology.gingester.core.Configuration;
import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.transformers.base.common.ToJsonBase;
import b.nana.technology.gingester.transformers.base.transformers.inputstream.Gunzip;
import b.nana.technology.gingester.transformers.base.transformers.inputstream.ToString;
import b.nana.technology.gingester.transformers.base.transformers.json.AddContext;
import b.nana.technology.gingester.transformers.base.transformers.json.Wrap;
import b.nana.technology.gingester.transformers.base.transformers.string.ToJson;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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
        AddContext addContext = new AddContext();

        Queue<JsonNode> results = new LinkedBlockingQueue<>();

        Gingester gingester = new Gingester();
        gingester.seed(gunzip, getClass().getResourceAsStream("/hello-world.ndjson.gz"));
        gingester.link(gunzip, toString);
        gingester.link(toString, toJson);
        gingester.link(toJson, wrap);
        gingester.link(wrap, addContext);
        gingester.link(addContext, results::add);
        gingester.run();

        Set<String> contexts = results.stream().map(jsonNode -> jsonNode.get("context").asText()).collect(Collectors.toSet());
        Set<String> messages = results.stream().map(jsonNode -> jsonNode.get("content").get("message").asText()).collect(Collectors.toSet());

        assertEquals(3, results.size());
        assertEquals(Set.of("1", "2", "3"), contexts);
        assertEquals(Set.of("Hello, World 1!", "Hello, World 2!", "Hello, World 3!"), messages);
    }

    @Test
    void testHelloWorldNdJsonGzFromConfiguration() throws IOException {

        Configuration configuration = Configuration.fromJson(getClass().getResourceAsStream("/hello-world.gingester.json"));
        Gingester gingester = configuration.build();

        Gunzip gunzip = gingester.getTransformer("InputStream.Gunzip", Gunzip.class);
        gingester.seed(gunzip, getClass().getResourceAsStream("/hello-world.ndjson.gz"));

        Queue<JsonNode> results = new LinkedBlockingQueue<>();
        AddContext addContext = gingester.getTransformer("Json.AddContext", AddContext.class);
        gingester.link(addContext, results::add);

        gingester.run();

        Set<String> contexts = results.stream().map(jsonNode -> jsonNode.get("context").asText()).collect(Collectors.toSet());
        Set<String> messages = results.stream().map(jsonNode -> jsonNode.get("content").get("message").asText()).collect(Collectors.toSet());

        assertEquals(3, results.size());
        assertEquals(Set.of("1", "2", "3"), contexts);
        assertEquals(Set.of("Hello, World 1!", "Hello, World 2!", "Hello, World 3!"), messages);
    }

    @Test
    void testHelloWorldNdJsonGzFromConfigurationWithToJsonParameters() throws IOException {

        Configuration configuration = Configuration.fromJson(getClass().getResourceAsStream("/hello-world-with-to-json-parameters.gingester.json"));
        Gingester gingester = configuration.build();

        Gunzip gunzip = gingester.getTransformer("InputStream.Gunzip", Gunzip.class);
        gingester.seed(gunzip, getClass().getResourceAsStream("/hello-world.ndjson.gz"));

        Queue<JsonNode> results = new LinkedBlockingQueue<>();
        AddContext addContext = gingester.getTransformer("Json.AddContext", AddContext.class);
        gingester.link(addContext, results::add);

        gingester.run();

        Set<String> contexts = results.stream().map(jsonNode -> jsonNode.get("context").asText()).collect(Collectors.toSet());
        Set<String> messages = results.stream().map(jsonNode -> jsonNode.get("content").get("message").asText()).collect(Collectors.toSet());

        assertEquals(3, results.size());
        assertEquals(Set.of("1", "2", "3"), contexts);
        assertEquals(Set.of("Hello, World 1!", "Hello, World 2!", "Hello, World 3!"), messages);
    }
}

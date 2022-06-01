package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;

import java.util.NoSuchElementException;

public final class Remove implements Transformer<JsonNode, JsonNode> {

    private static final Configuration CONFIGURATION = Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .options(Option.SUPPRESS_EXCEPTIONS)
            .build();

    private final JsonPath jsonPath;
    private final boolean optional;

    public Remove(Parameters parameters) {
        jsonPath = JsonPath.compile(parameters.path);
        optional = parameters.optional;
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<JsonNode> out) {
        DocumentContext documentContext = JsonPath.parse(in, CONFIGURATION);
        JsonNode result = documentContext.read(jsonPath);
        if (result != null) {
            documentContext.delete(jsonPath);
            out.accept(context, result);
        } else if (!optional) {
            throw new NoSuchElementException(jsonPath.getPath());
        }
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {

        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, path -> o("path", path));
                rule(JsonNode::isArray, array -> {
                    if (array.size() == 2 && array.get(1).asText().equals("optional")) {
                        return o("path", array.get(0), "optional", true);
                    } else {
                        throw new IllegalArgumentException("Invalid parameters: " + array);
                    }
                });
            }
        }

        public String path;
        public boolean optional;
    }
}

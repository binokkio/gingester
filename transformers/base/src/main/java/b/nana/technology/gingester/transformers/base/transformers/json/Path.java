package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;

import java.util.NoSuchElementException;

@Example(example = "$.hello", description = "Yield the JsonNode at key \"hello\", throw if missing")
@Example(example = "$.hello optional", description = "Yield the JsonNode at key \"hello\", ignore if missing")
public final class Path implements Transformer<JsonNode, JsonNode> {

    private static final Configuration CONFIGURATION = Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .options(Option.SUPPRESS_EXCEPTIONS)
            .build();

    private final String descriptionPrefix;
    private final JsonPath jsonPath;
    private final boolean optional;

    public Path(Parameters parameters) {
        descriptionPrefix = parameters.path + " :: ";
        jsonPath = JsonPath.compile(parameters.path);
        optional = parameters.optional;
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<JsonNode> out) throws NoSuchMethodException {
        DocumentContext documentContext = JsonPath.parse(in, CONFIGURATION);
        if (jsonPath.isDefinite()) {
            JsonNode result = documentContext.read(jsonPath);
            if (result != null) {
                out.accept(context.stash("description", descriptionPrefix + '0'), result);
            } else if (!optional) {
                throw new NoSuchElementException(descriptionPrefix);
            }
        } else {
            ArrayNode jsonNodes = documentContext.read(jsonPath);
            int i = 0;
            for (; i < jsonNodes.size(); i++) {
                out.accept(context.stash("description", descriptionPrefix + i), jsonNodes.get(i));
            }
            if (i == 0 && !optional) throw new NoSuchElementException(descriptionPrefix);
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

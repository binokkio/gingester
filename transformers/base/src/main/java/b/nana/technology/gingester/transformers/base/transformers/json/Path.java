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
public class Path implements Transformer<JsonNode, JsonNode> {

    private static final Configuration CONFIGURATION = Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .options(Option.SUPPRESS_EXCEPTIONS)
            .build();

    private final String description;
    private final String descriptionPrefix;
    private final JsonPath jsonPath;
    private final boolean optional;
    private final boolean remove;

    public Path(Parameters parameters) {
        this(parameters, false);
    }

    Path(Parameters parameters, boolean remove) {
        description = parameters.path;
        descriptionPrefix = description + " :: ";
        jsonPath = JsonPath.compile(parameters.path);
        optional = parameters.optional;
        this.remove = remove;
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<JsonNode> out) {
        DocumentContext documentContext = JsonPath.parse(in, CONFIGURATION);
        if (jsonPath.isDefinite()) {
            JsonNode result = documentContext.read(jsonPath);
            if (result != null) {
                if (remove) documentContext.delete(jsonPath);
                out.accept(context.stash("description", description), result);
            } else if (!optional) {
                throw new NoSuchElementException(description);
            }
        } else {
            ArrayNode jsonNodes = documentContext.read(jsonPath);
            if (!jsonNodes.isEmpty()) {
                if (remove) documentContext.delete(jsonPath);
                int i = 0;
                for (JsonNode jsonNode : jsonNodes) {
                    out.accept(context.stash("description", descriptionPrefix + i++), jsonNode);
                }
            } else if (!optional) {
                throw new NoSuchElementException(description);
            }
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

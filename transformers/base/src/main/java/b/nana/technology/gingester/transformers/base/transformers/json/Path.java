package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;

public final class Path implements Transformer<JsonNode, JsonNode> {

    private static final Configuration CONFIGURATION = Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .build();

    private final String descriptionPrefix;
    private final JsonPath jsonPath;

    public Path(Parameters parameters) {
        descriptionPrefix = parameters.path + " :: ";
        jsonPath = JsonPath.compile(parameters.path);
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<JsonNode> out) {
        DocumentContext documentContext = JsonPath.parse(in, CONFIGURATION);
        if (jsonPath.isDefinite()) {
            out.accept(context.stash("description", descriptionPrefix + '0'), documentContext.read(jsonPath));
        } else {
            ArrayNode jsonNodes = documentContext.read(jsonPath);
            for (int i = 0; i < jsonNodes.size(); i++) {
                out.accept(context.stash("description", descriptionPrefix + i), jsonNodes.get(i));
            }
        }
    }

    public static class Parameters {

        public String path;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String path) {
            this.path = path;
        }
    }
}

package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;

import java.util.List;
import java.util.stream.Collectors;

import static b.nana.technology.gingester.transformers.base.common.json.PathToPointer.jsonPathToPointer;

public final class ForceArrays implements Transformer<JsonNode, JsonNode> {

    private static final Configuration CONFIGURATION = Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .options(Option.AS_PATH_LIST)
            .build();

    private final List<JsonPath> paths;

    public ForceArrays(Parameters parameters) {
        paths = parameters.paths.stream().map(JsonPath::compile).collect(Collectors.toList());
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<JsonNode> out) throws Exception {
        DocumentContext documentContext = JsonPath.parse(in, CONFIGURATION);
        for (JsonPath path : paths) {
            ArrayNode results = documentContext.read(path);
            for (JsonNode result : results) {
                String jsonPath = result.asText();
                JsonPointer jsonPointer = JsonPointer.compile(jsonPathToPointer(jsonPath));
                JsonNode container = in.at(jsonPointer.head());
                if (container.isObject()) {
                    String targetName = jsonPointer.last().toString().substring(1);
                    JsonNode target = container.get(targetName);
                    if (!target.isArray()) {
                        ArrayNode wrapper = JsonNodeFactory.instance.arrayNode();
                        wrapper.add(target);
                        ((ObjectNode) container).set(targetName, wrapper);
                    }
                } else if (container.isArray()) {
                    int targetIndex = Integer.parseInt(jsonPointer.last().toString().substring(1));
                    JsonNode target = container.get(targetIndex);
                    if (!target.isArray()) {
                        ArrayNode wrapper = JsonNodeFactory.instance.arrayNode();
                        wrapper.add(target);
                        ((ArrayNode) container).set(targetIndex, wrapper);
                    }
                } else {
                    throw new IllegalStateException("Container is neither object nor array");
                }
            }
        }
        out.accept(context, in);
    }

    public static class Parameters {

        public List<String> paths;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(List<String> paths) {
            this.paths = paths;
        }
    }
}

package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.annotations.Description;
import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Description("Add input to target array and yield target")
@Example(example = "", description = "Add input at the end of the most recent explicit stash")
@Example(example = "0", description = "Add input at the beginning of the most recent explicit stash")
@Example(example = "target", description = "Add input at the end of the array stashed as \"target\"")
@Example(example = "1 target", description = "Add input at index 1 of the array stashed as \"target\"")
public final class Add implements Transformer<JsonNode, JsonNode> {

    private final int index;
    private final FetchKey fetchTarget;

    public Add(Parameters parameters) {
        index = parameters.index == null ? -1 : parameters.index;
        fetchTarget = parameters.target;
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<JsonNode> out) {
        ArrayNode arrayNode = (ArrayNode) context.require(fetchTarget);
        if (index != -1)
            arrayNode.set(index, in);
        else
            arrayNode.add(in);
        out.accept(context, arrayNode);
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {

        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isInt, index -> o("index", index));
                rule(JsonNode::isTextual, target -> o("target", target));
                rule(JsonNode::isArray, array -> {
                    if (array.size() != 2) throw new IllegalArgumentException("Invalid parameters: " + array);
                    return o("index", array.path(0), "target", array.path(1));
                });
            }
        }

        public Integer index = null;
        public FetchKey target = new FetchKey(1);
    }
}

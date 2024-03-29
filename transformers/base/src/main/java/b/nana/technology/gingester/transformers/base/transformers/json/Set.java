package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.annotations.Description;
import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Description("Set input on target and yield target")
@Example(example = "key", description = "Set input as \"key\" on the most recent ordinal stash")
@Example(example = "key target", description = "Set input as \"key\" on what is stashed as \"target\"")
public final class Set implements Transformer<JsonNode, JsonNode> {

    private final Template key;
    private final FetchKey fetchTarget;

    public Set(Parameters parameters) {
        key = Context.newTemplate(parameters.key);
        fetchTarget = parameters.target;
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<JsonNode> out) {
        ObjectNode objectNode = (ObjectNode) context.require(fetchTarget);
        objectNode.set(key.render(context, in), in);
        out.accept(context, objectNode);
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {

        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, key -> o("key", key));
                rule(JsonNode::isArray, array -> {
                    if (array.size() != 2) throw new IllegalArgumentException("Invalid parameters: " + array);
                    return o("key", array.path(0), "target", array.path(1));
                });
            }
        }

        public TemplateParameters key;
        public FetchKey target = new FetchKey(1);
    }
}

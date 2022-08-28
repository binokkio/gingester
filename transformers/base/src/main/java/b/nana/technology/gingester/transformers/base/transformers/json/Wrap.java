package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class Wrap implements Transformer<JsonNode, JsonNode> {

    private final Template key;

    public Wrap(Parameters parameters) {
        key = Context.newTemplate(parameters.key);
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<JsonNode> out) {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.set(key.render(context), in);
        out.accept(context, objectNode);
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, key -> o("key", key));
            }
        }

        public TemplateParameters key = new TemplateParameters("value");
    }
}

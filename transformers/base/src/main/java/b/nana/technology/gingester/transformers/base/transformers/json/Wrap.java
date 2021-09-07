package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Wrap implements Transformer<JsonNode, JsonNode> {

    private final String key;

    public Wrap(Parameters parameters) {
        key = parameters.key;
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<JsonNode> out) throws Exception {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.set(key, in);
        out.accept(context, objectNode);
    }

    public static class Parameters {

        public String key = "value";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String key) {
            this.key = key;
        }
    }
}

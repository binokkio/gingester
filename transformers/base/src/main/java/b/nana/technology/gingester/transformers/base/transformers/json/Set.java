package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Set implements Transformer<JsonNode, JsonNode> {

    private final String key;
    private final String value;

    public Set(Parameters parameters) {
        key = parameters.key;
        value = parameters.value;
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<JsonNode> out) throws Exception {
        ((ObjectNode) in).set(key, JsonNodeFactory.instance.pojoNode(context.fetch(value).findFirst().orElseThrow()));
        out.accept(context, in);
    }

    public static class Parameters {

        public String key;
        public String value = "stash";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String key) {
            this.key = key;
        }
    }
}

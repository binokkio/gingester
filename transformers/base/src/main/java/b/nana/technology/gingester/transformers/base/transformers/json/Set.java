package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class Set implements Transformer<JsonNode, JsonNode> {

    private final String key;
    private final String fetch;

    public Set(Parameters parameters) {
        key = parameters.key;
        fetch = parameters.fetch;
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<JsonNode> out) throws Exception {
        Object value = context.fetch(fetch).findFirst().orElseThrow();
        ((ObjectNode) in).set(key, value instanceof JsonNode ? (JsonNode) value : JsonNodeFactory.instance.pojoNode(value));
        out.accept(context, in);
    }

    public static class Parameters {

        public String key;
        public String fetch = "stash";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String key) {
            this.key = key;
        }
    }
}

package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Wrap extends Transformer<JsonNode, JsonNode> {

    private final String key;

    public Wrap() {
        this(new Parameters());
    }

    public Wrap(Parameters parameters) {
        super(parameters);
        key = parameters.key;
    }

    @Override
    protected void transform(Context context, JsonNode input) {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.set(key, input);
        emit(context, objectNode);
    }

    public static class Parameters {

        public String key = "content";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String key) {
            this.key = key;
        }
    }
}

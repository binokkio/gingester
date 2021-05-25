package b.nana.technology.gingester.transformers.base.common.json.insert;

import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class InsertBase extends Transformer<JsonNode, JsonNode> {

    public InsertBase(Parameters parameters) {
        super(parameters);
    }

    protected ObjectNode prepare(JsonNode jsonNode, String... keys) {

        JsonNode pointer = jsonNode;
        for (int i = 0; i < keys.length - 1; i++) {
            JsonNode next = pointer.path(keys[i]);
            if (next.isMissingNode()) {
                if (!pointer.isObject()) {
                    throw new IllegalArgumentException("1");  // TODO
                } else {
                    next = JsonNodeFactory.instance.objectNode();
                    ((ObjectNode) pointer).set(keys[i], next);
                }
            }
            pointer = next;
        }

        if (!pointer.isObject()) {
            throw new IllegalArgumentException("2");  // TODO
        }

        return (ObjectNode) pointer;
    }

    public static class Parameters {

    }
}

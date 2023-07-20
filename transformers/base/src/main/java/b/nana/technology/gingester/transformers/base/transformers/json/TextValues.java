package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;

public final class TextValues implements Transformer<JsonNode, JsonNode> {

    @Override
    public void transform(Context context, JsonNode in, Receiver<JsonNode> out) {
        textValues(in);
        out.accept(context, in);
    }

    private void textValues(JsonNode jsonNode) {
        if (jsonNode.isObject()) {
            Iterator<String> keys = jsonNode.fieldNames();
            while (keys.hasNext()) {
                String key = keys.next();
                JsonNode value = jsonNode.get(key);
                if (value.isContainerNode()) {
                    textValues(value);
                } else if (!value.isTextual()) {
                    ((ObjectNode) jsonNode).put(key, value.asText());
                }
            }
        } else if (jsonNode.isArray()) {
            for (int i = 0; i < jsonNode.size(); i++) {
                JsonNode value = jsonNode.get(i);
                if (value.isContainerNode()) {
                    textValues(value);
                } else if (!value.isTextual()) {
                    ((ArrayNode) jsonNode).set(i, value.asText());
                }
            }
        }
    }
}

package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.annotations.Description;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.transformers.base.common.json.ToJsonTransformer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;

@Description("Interpret nested (internal) JSON strings")
public class Internpret extends ToJsonTransformer<JsonNode> {

    public Internpret(Parameters parameters) {
        super(parameters);
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<JsonNode> out) throws Exception {
        interpret(in);
        out.accept(context, in);
    }

    private void interpret(JsonNode jsonNode) {
        if (jsonNode.isObject()) {
            Iterator<String> keys = jsonNode.fieldNames();
            while (keys.hasNext()) {
                String key = keys.next();
                JsonNode value = jsonNode.get(key);
                if (value.isTextual()) {
                    JsonNode interpretation = interpret(value.textValue());
                    if (interpretation != null) {
                        ((ObjectNode) jsonNode).set(key, interpretation);
                    }
                } else {
                    interpret(value);
                }
            }
        } else if (jsonNode.isArray()) {
            for (int i = 0; i < jsonNode.size(); i++) {
                JsonNode value = jsonNode.get(i);
                if (value.isTextual()) {
                    JsonNode interpretation = interpret(value.textValue());
                    if (interpretation != null) {
                        ((ArrayNode) jsonNode).set(i, interpretation);
                    }
                } else {
                    interpret(value);
                }
            }
        }
    }

    // TODO this method is probably worth optimizing
    private JsonNode interpret(String string) {
        switch (string) {

            case "":
            case "null":
                return JsonNodeFactory.instance.nullNode();

            case "true": return JsonNodeFactory.instance.booleanNode(true);
            case "false": return JsonNodeFactory.instance.booleanNode(false);
        }
        char firstChar = string.charAt(0);
        if (firstChar == '{' || firstChar == '[' || firstChar == '-' || Character.isDigit(firstChar)) {
            try {
                return getObjectReader().readTree(string);
            } catch (JsonProcessingException e) {
                return null;
            }
        }
        return null;
    }
}

package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SnakeCase implements Transformer<JsonNode, JsonNode> {

    private static final Pattern CAPITAL_PATTERN = Pattern.compile("([A-Z])");

    @Override
    public void transform(Context context, JsonNode in, Receiver<JsonNode> out) {
        out.accept(context, transform(in));
    }

    private JsonNode transform(JsonNode jsonNode) {

        if (jsonNode.isObject()) {

            ObjectNode replacement = JsonNodeFactory.instance.objectNode();

            Iterator<Map.Entry<String, JsonNode>> iterator = jsonNode.fields();
            while (iterator.hasNext()) {

                Map.Entry<String, JsonNode> entry = iterator.next();
                String fieldName = entry.getKey();
                JsonNode value = entry.getValue();

                replacement.set(snakeCase(fieldName), transform(value));
            }

            return replacement;

        } else if (jsonNode.isArray()) {

            ArrayNode replacement = JsonNodeFactory.instance.arrayNode(jsonNode.size());

            for (JsonNode value : jsonNode) {
                replacement.add(transform(value));
            }

            return replacement;

        } else {
            return jsonNode;
        }
    }

    private String snakeCase(String string) {
        Matcher matcher = CAPITAL_PATTERN.matcher(string);
        return matcher.replaceAll("_$1").toLowerCase(Locale.ENGLISH);
    }
}

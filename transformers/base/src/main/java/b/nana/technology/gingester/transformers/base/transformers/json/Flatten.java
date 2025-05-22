package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;

@Example(example = "", description = "Produces keys like object.array[0]")
@Example(example = "'/'", description = "Produces keys like object/array/0")
public final class Flatten implements Transformer<JsonNode, JsonNode> {

    private final String join;

    public Flatten(Parameters parameters) {
        join = parameters.join;
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<JsonNode> out) {
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        walk(in, new ArrayDeque<>(), result::set);
        out.accept(context, result);
    }

    private void walk(JsonNode jsonNode, Deque<String> keys, BiConsumer<String, JsonNode> valueConsumer) {

        if (jsonNode.isObject()) {

            Iterator<Map.Entry<String, JsonNode>> iterator = jsonNode.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                keys.add(join != null ? entry.getKey() : keys.isEmpty() ? entry.getKey() : '.' + entry.getKey());
                walk(entry.getValue(), keys, valueConsumer);
                keys.removeLast();
            }

        } else if (jsonNode.isArray()) {

            for (int i = 0; i < jsonNode.size(); i++) {
                keys.add(join != null ? Integer.toString(i) : "[" + i + "]");
                walk(jsonNode.get(i), keys, valueConsumer);
                keys.removeLast();
            }

        } else {
            valueConsumer.accept(String.join(join != null ? join : "", keys), jsonNode);
        }
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order("join")
    public static class Parameters {
        public String join;
    }
}

package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ObjectToArray extends Transformer<JsonNode, JsonNode> {

    @Override
    protected void transform(Context context, JsonNode input) throws Exception {
        ObjectNode objectNode = (ObjectNode) input;
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode(objectNode.size());
        objectNode.fields().forEachRemaining(entry -> {
            ObjectNode item = arrayNode.addObject();
            item.put("key", entry.getKey());
            item.put("value", entry.getValue());
        });
    }
}

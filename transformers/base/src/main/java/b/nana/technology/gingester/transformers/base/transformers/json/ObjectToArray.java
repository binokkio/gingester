package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ObjectToArray implements Transformer<ObjectNode, ArrayNode> {

    @Override
    public void transform(Context context, ObjectNode in, Receiver<ArrayNode> out) throws Exception {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode(in.size());
        in.fields().forEachRemaining(entry -> {
            ObjectNode item = arrayNode.addObject();
            item.put("key", entry.getKey());
            item.set("value", entry.getValue());
        });
    }
}

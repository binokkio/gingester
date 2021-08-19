package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;

public class ObjectToArray extends Transformer<JsonNode, JsonNode> {

    private final String parentPointer;
    private final String objectKey;

    public ObjectToArray(Parameters parameters) {
        super(parameters);
        int lastIndexOfSeparator = parameters.objectPointer.lastIndexOf('/');

        parentPointer = lastIndexOfSeparator == -1 ? "" :
                parameters.objectPointer.substring(0, lastIndexOfSeparator);

        objectKey = lastIndexOfSeparator == -1 ? null :
                parameters.objectPointer.substring(lastIndexOfSeparator);
    }

    @Override
    protected void transform(Context context, JsonNode input) throws Exception {
        ObjectNode parent = (ObjectNode) input.at(parentPointer);  // TODO deal with non object nodes
        JsonNode object = objectKey == null ? parent : parent.get(objectKey);
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        Iterator<String> keys = object.fieldNames();
        while (keys.hasNext()) {
            String key = keys.next();
            ObjectNode wrapper = JsonNodeFactory.instance.objectNode();
            wrapper.put("key", key);
            wrapper.set("value", object.get(key));
            array.add(wrapper);
        }
        if (objectKey == null) {
            emit(context, array);
        } else {
            parent.set(objectKey, array);
            emit(context, parent);
        }
    }

    public static class Parameters {

        public String objectPointer;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String objectPointer) {
            this.objectPointer = objectPointer;
        }
    }
}

package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Wrap extends Transformer<JsonNode, JsonNode> {

    @Override
    protected void setup(Setup setup) {
        setup.syncInputs();
    }

    @Override
    protected void transform(Context context, JsonNode input) {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.set("content", input);
        emit(context, objectNode);
    }
}

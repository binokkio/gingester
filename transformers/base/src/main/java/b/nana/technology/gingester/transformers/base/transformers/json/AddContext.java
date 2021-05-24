package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AddContext extends Transformer<JsonNode, JsonNode> {

    @Override
    protected void setup(Setup setup) {
        setup.preferUpstreamSync();
    }

    @Override
    protected void transform(Context context, JsonNode input) {
        ((ObjectNode) input).put("context", context.getDescription());
        emit(context, input);
    }
}

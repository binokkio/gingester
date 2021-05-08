package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.databind.JsonNode;

public class Copy extends Transformer<JsonNode, JsonNode> {

    @Override
    protected void transform(Context context, JsonNode input) {
        emit(context, input.deepCopy());
    }
}

package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.databind.JsonNode;

public class ToInteger extends Transformer<JsonNode, Long> {
    @Override
    protected void transform(Context context, JsonNode input) {
        emit(context, input.asLong());
    }
}

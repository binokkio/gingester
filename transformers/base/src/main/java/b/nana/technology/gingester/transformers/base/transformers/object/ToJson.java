package b.nana.technology.gingester.transformers.base.transformers.object;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.transformers.base.common.json.ToJsonTransformer;
import com.fasterxml.jackson.databind.JsonNode;

public final class ToJson extends ToJsonTransformer<Object> {

    public ToJson(Parameters parameters) {
        super(parameters);
    }

    @Override
    public void transform(Context context, Object in, Receiver<JsonNode> out) throws Exception {
        out.accept(context, getObjectMapper().valueToTree(in));
    }
}

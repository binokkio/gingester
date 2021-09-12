package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.transformers.base.common.json.ToJsonTransformer;
import com.fasterxml.jackson.databind.JsonNode;

public class ToJson extends ToJsonTransformer<String> {

    public ToJson(Parameters parameters) {
        super(parameters);
    }

    @Override
    public void transform(Context context, String in, Receiver<JsonNode> out) throws Exception {
        out.accept(context, getObjectReader().readTree(in));
    }
}

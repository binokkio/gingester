package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.transformers.base.common.json.ToJsonTransformer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.InputStream;

public class ToJson extends ToJsonTransformer<InputStream> {

    public ToJson(ToJsonTransformer.Parameters parameters) {
        super(parameters);
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<JsonNode> out) throws Exception {
        out.accept(context, getObjectReader().readTree(in));
    }
}

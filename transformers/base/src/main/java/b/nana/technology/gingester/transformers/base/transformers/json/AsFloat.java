package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;

public final class AsFloat implements Transformer<JsonNode, Float> {

    @Override
    public void transform(Context context, JsonNode in, Receiver<Float> out) throws Exception {
        out.accept(context, (float) in.asDouble());
    }
}

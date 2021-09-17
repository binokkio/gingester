package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;

public final class AsBoolean implements Transformer<JsonNode, Boolean> {

    @Override
    public void transform(Context context, JsonNode in, Receiver<Boolean> out) throws Exception {
        out.accept(context, in.asBoolean());
    }
}

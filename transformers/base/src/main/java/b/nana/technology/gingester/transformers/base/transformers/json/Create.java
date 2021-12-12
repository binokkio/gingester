package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;

public final class Create implements Transformer<Object, JsonNode> {

    private final JsonNode payload;

    public Create(Parameters parameters) {
        payload = parameters.payload;
    }

    @Override
    public void transform(Context context, Object in, Receiver<JsonNode> out) throws Exception {
        out.accept(context, payload);
    }

    public static class Parameters {

        public JsonNode payload;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(JsonNode payload) {
            this.payload = payload;
        }
    }
}

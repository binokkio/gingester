package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;

public final class Create implements Transformer<Object, JsonNode> {

    private final int count;
    private final JsonNode payload;

    public Create(Parameters parameters) {
        count = parameters.count;
        payload = parameters.payload;
    }

    @Override
    public void transform(Context context, Object in, Receiver<JsonNode> out) throws Exception {
        for (int i = 0; i < count; i++) {
            out.accept(context.stash("description", count), payload);
        }
    }

    public static class Parameters {

        public int count = 1;
        public JsonNode payload;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(JsonNode payload) {
            if (payload.size() == 2 && payload.has("count") && payload.has("payload")) {
                this.count = payload.get("count").asInt();
                this.payload = payload.get("payload");
            } else {
                this.payload = payload;
            }
        }
    }
}

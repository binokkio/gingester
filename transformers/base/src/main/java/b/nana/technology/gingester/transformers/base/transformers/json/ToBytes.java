package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;

@Pure
public final class ToBytes implements Transformer<JsonNode, byte[]> {

    private final boolean pretty;

    public ToBytes(Parameters parameters) {
        pretty = parameters.pretty;
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<byte[]> out) throws Exception {
        if (pretty) {
            out.accept(context, in.toPrettyString().getBytes(StandardCharsets.UTF_8));
        } else {
            out.accept(context, in.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    public static class Parameters {

        public boolean pretty;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(boolean pretty) {
            this.pretty = pretty;
        }
    }
}

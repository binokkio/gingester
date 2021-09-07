package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ToInputStream implements Transformer<JsonNode, InputStream> {

    private final boolean pretty;

    public ToInputStream(Parameters parameters) {
        pretty = parameters.pretty;
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<InputStream> out) throws Exception {
        if (pretty) {
            out.accept(context, new ByteArrayInputStream(in.toPrettyString().getBytes(StandardCharsets.UTF_8)));
        } else {
            out.accept(context, new ByteArrayInputStream(in.toString().getBytes(StandardCharsets.UTF_8)));
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

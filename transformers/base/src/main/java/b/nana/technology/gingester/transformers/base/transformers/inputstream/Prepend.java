package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;

public final class Prepend implements Transformer<InputStream, InputStream> {

    private final byte[] prepend;

    public Prepend(Parameters parameters) {
        prepend = parameters.prepend.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<InputStream> out) {
        out.accept(context, new SequenceInputStream(new ByteArrayInputStream(prepend), in));
    }

    public static class Parameters {

        public String prepend = "\n";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String prepend) {
            this.prepend = prepend;
        }
    }
}

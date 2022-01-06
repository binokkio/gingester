package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.InputStream;

public final class Drain implements Transformer<InputStream, InputStream> {

    private final byte[] buffer;

    public Drain(Parameters parameters) {
        buffer = new byte[parameters.bufferSize];
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<InputStream> out) throws Exception {
        while (in.read(buffer) != -1);
        out.accept(context, in);
    }

    public static class Parameters {

        public int bufferSize = 8192;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(int bufferSize) {
            this.bufferSize = bufferSize;
        }
    }
}

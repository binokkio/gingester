package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.InputStream;

public final class Skip implements Transformer<InputStream, InputStream> {

    private final long skip;

    public Skip(Parameters parameters) {
        skip = parameters.skip;
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<InputStream> out) throws Exception {
        in.skip(skip);
        out.accept(context, in);
    }

    public static class Parameters {

        public long skip = Long.MAX_VALUE;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(int skip) {
            this.skip = skip;
        }
    }
}

package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

public class Append implements Transformer<String, String> {

    private final String append;

    public Append(Parameters parameters) {
        append = parameters.append;
    }

    @Override
    public void transform(Context context, String in, Receiver<String> out) {
        out.accept(context, in + append);
    }

    public static class Parameters {

        public String append;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String append) {
            this.append = append;
        }
    }
}

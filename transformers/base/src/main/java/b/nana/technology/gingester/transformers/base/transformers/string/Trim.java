package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class Trim implements Transformer<String, String> {

    private final boolean dropEmpty;

    public Trim(Parameters parameters) {
        dropEmpty = parameters.dropEmpty;
    }

    @Override
    public void transform(Context context, String in, Receiver<String> out) {
        String result = in.trim();
        if (!result.isEmpty() || !dropEmpty) {
            out.accept(context, result);
        }
    }

    public static class Parameters {

        public boolean dropEmpty;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(boolean dropEmpty) {
            this.dropEmpty = dropEmpty;
        }
    }
}

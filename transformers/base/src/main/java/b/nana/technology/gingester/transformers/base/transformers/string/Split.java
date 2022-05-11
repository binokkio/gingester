package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class Split implements Transformer<String, String> {

    private final char delimiter;

    public Split(Parameters parameters) {
        delimiter = parameters.delimiter;
    }

    @Override
    public void transform(Context context, String in, Receiver<String> out) {
        int counter = 0;
        int start = 0;
        for (int i = 0; i < in.length(); i++) {
            if (in.charAt(i) == delimiter) {
                out.accept(context.stash("description", counter++), in.substring(start, i));
                start = i + 1;
            }
        }
        out.accept(context.stash("description", counter), in.substring(start));
    }

    public static class Parameters {

        public char delimiter;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(char delimiter) {
            this.delimiter = delimiter;
        }
    }
}

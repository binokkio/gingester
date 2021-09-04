package b.nana.technology.gingester.test.transformers;

import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import com.fasterxml.jackson.annotation.JsonCreator;

public class Generate implements Transformer<Void, String> {

    private final String string;
    private final int count;

    public Generate(Parameters parameters) {
        string = parameters.string;
        count = parameters.count;
    }

    @Override
    public void transform(Context context, Void in, Receiver<String> out) {
        for (int i = 0; i < count; i++) {
            out.accept(context, string);
        }
    }

    private static class Parameters {
        public String string;
        public int count = 1;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String string) {
            this.string = string;
        }
    }
}

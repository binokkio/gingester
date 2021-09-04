package b.nana.technology.gingester.transformers.base.transformers;

import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

public class GenerateString implements Transformer<Object, String> {

    private final String string;
    private final int count;

    public GenerateString(Parameters parameters) {
        string = parameters.string;
        count = parameters.count;
    }

    @Override
    public void transform(Context context, Object in, Receiver<String> out) {
        for (int i = 0; i < count; i++) {
            out.accept(context, string);
        }
    }

    public static class Parameters {
        public String string;
        public int count = 1;
    }
}

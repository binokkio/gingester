package b.nana.technology.transformers.base;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;

import java.util.function.BiConsumer;

public class GenerateString implements Transformer<Object, String> {

    private final String string;
    private final int count;

    public GenerateString(Parameters parameters) {
        string = parameters.string;
        count = parameters.count;
    }

    @Override
    public void transform(Context context, Object input, BiConsumer<Context, String> output) {
        for (int i = 0; i < count; i++) {
            output.accept(context, string);
        }
    }

    public static class Parameters {
        public String string;
        public int count = 1;
    }
}

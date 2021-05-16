package b.nana.technology.gingester.test.transformers;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;

public class Generate extends Transformer<Void, String> {

    private final String payload;
    private final int count;

    public Generate(String payload) {
        this.payload = payload;
        this.count = 1;
    }

    public Generate(Parameters parameters) {
        super(parameters);
        payload = parameters.payload;
        count = parameters.count;
    }

    @Override
    protected void transform(Context context, Void input) {
        for (int i = 1; i <= count; i++) {
            emit(context.extend(this).description(i), payload);
        }
    }

    private static class Parameters {
        public String payload;
        public int count;
    }
}

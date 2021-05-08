package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;

public class Generate extends Transformer<Void, String> {

    private final String payload;
    private final int count;

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

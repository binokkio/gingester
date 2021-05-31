package b.nana.technology.gingester.test.transformers;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

public class Generate extends Transformer<Void, String> {

    private final String string;
    private final int count;

    public Generate(String string) {
        this(new Parameters(string));
    }

    public Generate(Parameters parameters) {
        super(parameters);
        string = parameters.string;
        count = parameters.count;
    }

    @Override
    protected void transform(Context context, Void input) {
        for (int i = 0; i < count; i++) {
            emit(context.extend(this).description(i), string);
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

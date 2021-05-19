package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;

public class Generate extends Transformer<Object, String> {

    private final String payload;
    private final int count;

    public Generate(Parameters parameters) {
        super(parameters);
        payload = parameters.payload;
        count = parameters.count;
    }

    @Override
    protected void transform(Context context, Object input) {
        for (int i = 1; i <= count; i++) {
            emit(context.extend(this).description(i).details(Map.of("generate-counter", i)), payload);
        }
    }

    public static class Parameters {

        public String payload;
        public int count = 1;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String payload) {
            this.payload = payload;
        }
    }
}

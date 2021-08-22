package b.nana.technology.gingester.transformers.base.transformers.integer;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

public class GenerateRange extends Transformer<Object, Long> {

    private final long from;
    private final long to;

    public GenerateRange(Parameters parameters) {
        from = parameters.from;
        to = parameters.to;
    }

    @Override
    protected void transform(Context context, Object input) throws Exception {
        for (long i = from; i <= to; i++) {
            emit(context.extend(this).description(Long.toString(i)), i);
        }
    }

    public static class Parameters {

        public long from = 0;
        public long to;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(long to) {
            this.to = to;
        }
    }
}

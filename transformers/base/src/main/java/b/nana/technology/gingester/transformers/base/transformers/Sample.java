package b.nana.technology.gingester.transformers.base.transformers;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.ContextMap;
import b.nana.technology.gingester.core.Passthrough;
import com.fasterxml.jackson.annotation.JsonCreator;

public class Sample<T> extends Passthrough<T> {

    private final ContextMap<MutableLong> contextMap = new ContextMap<>();
    private final long divider;

    public Sample(Parameters parameters) {
        super(parameters);
        divider = parameters.divider;
    }

    @Override
    protected void prepare(Context context) throws Exception {
        contextMap.put(context, new MutableLong());
    }

    @Override
    protected void transform(Context context, T input) throws Exception {
        if (contextMap.require(context).getAndIncrement() % divider == 0) {
            emit(context, input);
        }
    }

    @Override
    protected void finish(Context context) {
        contextMap.requireRemove(context);
    }

    private static class MutableLong {

        private long value;

        public long getAndIncrement() {
            long moribund = value;
            value++;
            return moribund;
        }
    }

    public static class Parameters {

        public long divider = 10;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(long divider) {
            this.divider = divider;
        }
    }
}

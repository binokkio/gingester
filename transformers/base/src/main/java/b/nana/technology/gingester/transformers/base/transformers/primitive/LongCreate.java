package b.nana.technology.gingester.transformers.base.transformers.primitive;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

@Names(1)
public final class LongCreate implements Transformer<Object, Long> {

    private final Long value;

    public LongCreate(Parameters parameters) {
        value = parameters.value;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Long> out) {
        out.accept(context, value);
    }

    public static class Parameters {

        public long value;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(long value) {
            this.value = value;
        }
    }
}

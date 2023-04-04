package b.nana.technology.gingester.core.transformers.primitive;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import static java.util.Objects.requireNonNull;

@Names(1)
public final class LongDef implements Transformer<Object, Long> {

    private final Long value;

    public LongDef(Parameters parameters) {
        value = requireNonNull(parameters.value, "LongDef requires explicit value");
    }

    @Override
    public void transform(Context context, Object in, Receiver<Long> out) {
        out.accept(context, value);
    }

    public static class Parameters {

        public Long value;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(Long value) {
            this.value = value;
        }
    }
}

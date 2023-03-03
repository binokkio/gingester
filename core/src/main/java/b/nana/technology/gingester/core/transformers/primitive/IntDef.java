package b.nana.technology.gingester.core.transformers.primitive;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import static java.util.Objects.requireNonNull;

@Names(1)
public final class IntDef implements Transformer<Object, Integer> {

    private final Integer value;

    public IntDef(Parameters parameters) {
        value = requireNonNull(parameters.value, "IntDef requires explicit value");
    }

    @Override
    public void transform(Context context, Object in, Receiver<Integer> out) {
        out.accept(context, value);
    }

    public static class Parameters {

        public Integer value;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(Integer value) {
            this.value = value;
        }
    }
}

package b.nana.technology.gingester.core.transformers.primitive;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import static java.util.Objects.requireNonNull;

@Names(1)
public final class FloatDef implements Transformer<Object, Float> {

    private final Float value;

    public FloatDef(Parameters parameters) {
        value = requireNonNull(parameters.value, "FloatDef requires explicit value");
    }

    @Override
    public void transform(Context context, Object in, Receiver<Float> out) {
        out.accept(context, value);
    }

    public static class Parameters {

        public Float value;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(Float value) {
            this.value = value;
        }
    }
}

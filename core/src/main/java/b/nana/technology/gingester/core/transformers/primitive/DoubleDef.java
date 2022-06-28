package b.nana.technology.gingester.core.transformers.primitive;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import static java.util.Objects.requireNonNull;

@Names(1)
public final class DoubleDef implements Transformer<Object, Double> {

    private final Double value;

    public DoubleDef(Parameters parameters) {
        value = requireNonNull(parameters.value, "DoubleDef requires explicit value");
    }

    @Override
    public void transform(Context context, Object in, Receiver<Double> out) {
        out.accept(context, value);
    }

    public static class Parameters {

        public Double value;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(Double value) {
            this.value = value;
        }
    }
}

package b.nana.technology.gingester.core.transformers.primitive;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import static java.util.Objects.requireNonNull;

@Names(1)
public final class BooleanDef implements Transformer<Object, Boolean> {

    private final Boolean value;

    public BooleanDef(Parameters parameters) {
        value = requireNonNull(parameters.value, "BooleanDef requires explicit value");
    }

    @Override
    public void transform(Context context, Object in, Receiver<Boolean> out) throws Exception {
        out.accept(context, value);
    }

    public static class Parameters {

        public Boolean value;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(Boolean value) {
            this.value = value;
        }
    }
}

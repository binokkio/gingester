package b.nana.technology.gingester.core.transformers.stash;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.InputStasher;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

@Names(1)
@Passthrough
public final class Stash implements Transformer<Object, Object>, InputStasher {

    private final String name;

    public Stash(Parameters parameters) {

        if (!Character.isLowerCase(parameters.name.charAt(0)))
            throw new IllegalArgumentException("Stash name must start with lowercase character: " + parameters.name);

        name = parameters.name;
    }

    @Override
    public String getInputStashName() {
        return name;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {
        out.accept(context.stash(name, in), in);
    }

    public static class Parameters {

        public String name = "stash";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String name) {
            this.name = name;
        }
    }
}

package b.nana.technology.gingester.core.transformers.stash;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.StashDetails;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

@Names(1)
@Passthrough
public final class Stash implements Transformer<Object, Object> {

    private final String key;

    public Stash(Parameters parameters) {

        if (!Character.isLowerCase(parameters.key.charAt(0)))
            throw new IllegalArgumentException("Stash key must start with lowercase character: " + parameters.key);

        key = parameters.key;
    }

    @Override
    public StashDetails getStashDetails() {
        return StashDetails.ofOrdinal(key, "__input__");
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {
        out.accept(context.stash(key, in), in);
    }

    public static class Parameters {

        public String key = "stash";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String key) {
            this.key = key;
        }
    }
}

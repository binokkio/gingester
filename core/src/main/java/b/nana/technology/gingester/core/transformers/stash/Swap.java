package b.nana.technology.gingester.core.transformers.stash;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.StashDetails;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

@Names(1)
public final class Swap implements Transformer<Object, Object> {

    private final String name;
    private final FetchKey fetchKey;

    public Swap(Parameters parameters) {
        name = parameters.name;
        fetchKey = new FetchKey(name);
        if (fetchKey.isOrdinal()) {
            throw new IllegalArgumentException("Can't swap ordinal stash");
        } else if (fetchKey.getNames().length > 1) {
            throw new IllegalArgumentException("Can't swap nested stash");
        }
    }

    @Override
    public StashDetails getStashDetails() {
        return StashDetails.ofExplicit(name, "__input__");
    }

    @Override
    public Object getOutputType() {
        return fetchKey;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {
        out.accept(
                context.stash(name, in),
                context.require(fetchKey)
        );
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

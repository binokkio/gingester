package b.nana.technology.gingester.core.transformers.stash;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.OutputFetcher;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

@Names(1)
public final class Fetch implements Transformer<Object, Object>, OutputFetcher {

    private final FetchKey fetchKey;

    public Fetch(Parameters parameters) {
        fetchKey = new FetchKey(parameters.stash);
    }

    @Override
    public FetchKey getOutputStashName() {
        return fetchKey;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {
        out.accept(
                context,
                context.require(fetchKey)
        );
    }

    public static class Parameters {

        public String stash = "stash";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String stash) {
            this.stash = stash;
        }
    }
}

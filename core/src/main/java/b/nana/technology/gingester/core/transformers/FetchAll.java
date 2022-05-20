package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import static java.util.Objects.requireNonNull;

@Names(1)
public final class FetchAll implements Transformer<Object, Object> {  // TODO implement OutputFetcher and support @Pure bridges

    private final FetchKey fetchKey;

    public FetchAll(Parameters parameters) {
        fetchKey = requireNonNull(parameters.stash);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        context.fetchAll(fetchKey).forEach(item -> out.accept(context, item));
    }

    public static class Parameters {

        public FetchKey stash = new FetchKey("stash");

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(FetchKey stash) {
            this.stash = stash;
        }
    }
}

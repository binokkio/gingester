package b.nana.technology.gingester.transformers.base.transformers.index;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;

@Names(1)
public final class Lookup implements Transformer<Object, Object> {

    private final String stash;

    public Lookup(Parameters parameters) {
        stash = parameters.stash;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        Map<?, ?> index = (Map<?, ?>) context.fetch(stash).findFirst().orElseThrow();
        Object result = index.get(in);
        out.accept(context, result);
    }

    public static class Parameters {

        public String stash = "index";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String stash) {
            this.stash = stash;
        }
    }
}

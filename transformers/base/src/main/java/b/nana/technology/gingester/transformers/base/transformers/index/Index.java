package b.nana.technology.gingester.transformers.base.transformers.index;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformers.Fetch;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.*;

@Names(1)
public final class Index implements Transformer<Object, Map<Object, Object>> {

    private final ContextMap<Map<Object, Object>> indices = new ContextMap<>();

    private final String[] fetch;
    private final String stash;
    private final boolean throwOnEmptyFetch;

    public Index(Parameters parameters) {
        fetch = Fetch.parseStashName(parameters.fetch);
        stash = parameters.stash;
        throwOnEmptyFetch = parameters.throwOnEmptyFetch;
    }

    @Override
    public void setup(SetupControls controls) {
        controls.syncs(Collections.singletonList("__seed__"));
    }

    @Override
    public void prepare(Context context, Receiver<Map<Object, Object>> out) {
        indices.put(context, new HashMap<>());
    }

    @Override
    public void transform(Context context, Object in, Receiver<Map<Object, Object>> out) throws Exception {
        Optional<Object> value = context.fetch(fetch).findFirst();
        if (value.isPresent()) {
            indices.act(context, map -> map.put(in, value.get()));
        } else if (throwOnEmptyFetch) {
            throw new NoSuchElementException(String.join("/", fetch));
        }
    }

    @Override
    public void finish(Context context, Receiver<Map<Object, Object>> out) {
        Map<Object, Object> index = indices.remove(context);
        out.accept(context.stash(stash, index), index);
    }

    public static class Parameters {

        public String fetch = "stash";
        public String stash = "index";
        public boolean throwOnEmptyFetch = false;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String fetch) {
            this.fetch = fetch;
        }
    }
}

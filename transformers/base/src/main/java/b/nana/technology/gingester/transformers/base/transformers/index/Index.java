package b.nana.technology.gingester.transformers.base.transformers.index;

import b.nana.technology.gingester.core.annotations.Names;
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
    private final boolean throwOnCollision;
    private final boolean preserveInsertOrder;

    public Index(Parameters parameters) {
        fetch = Fetch.parseStashName(parameters.fetch);
        stash = parameters.stash;
        throwOnEmptyFetch = parameters.throwOnEmptyFetch;
        throwOnCollision = parameters.throwOnCollision;
        preserveInsertOrder = parameters.preserveInsertOrder;
    }

    @Override
    public void prepare(Context context, Receiver<Map<Object, Object>> out) {
        indices.put(context, preserveInsertOrder ? new LinkedHashMap<>() : new HashMap<>());
    }

    @Override
    public void transform(Context context, Object in, Receiver<Map<Object, Object>> out) throws Exception {
        Optional<Object> value = context.fetch(fetch).findFirst();
        if (value.isPresent()) {
            indices.act(context, map -> {
                Object collision = map.put(in, value.get());
                if (collision != null && throwOnCollision) {
                    throw new IllegalStateException("Key already present in index: " + in);
                }
            });
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
        public boolean throwOnCollision = false;
        public boolean preserveInsertOrder = false;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String fetch) {
            this.fetch = fetch;
        }
    }
}

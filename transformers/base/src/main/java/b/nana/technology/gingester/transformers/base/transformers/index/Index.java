package b.nana.technology.gingester.transformers.base.transformers.index;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Names(1)
public final class Index implements Transformer<Object, Map<Object, Object>> {

    private final ContextMap<Map<Object, Object>> indices = new ContextMap<>();

    private final String stash;

    public Index(Parameters parameters) {
        stash = parameters.stash;
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
        indices.act(context, map -> map.put(in, context.fetch(stash).findFirst().orElseThrow()));
    }

    @Override
    public void finish(Context context, Receiver<Map<Object, Object>> out) {
        Map<Object, Object> index = indices.remove(context);
        out.accept(context.stash("index", index), index);
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

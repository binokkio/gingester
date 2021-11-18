package b.nana.technology.gingester.transformers.base.transformers.util;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformers.Fetch;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Names(1)
public final class Latch implements Transformer<Object, Object> {

    private final ContextMap<Map<Object, Object>> state = new ContextMap<>();

    private final String[] keyStash;
    private final String[] valueStash;

    public Latch(Parameters parameters) {
        keyStash = parameters.key == null ? null : Fetch.parseStashName(parameters.key);
        valueStash = Fetch.parseStashName(parameters.value);
    }

    @Override
    public void setup(SetupControls controls) {
        controls.syncs(Collections.singletonList("__seed__"));
    }

    @Override
    public void prepare(Context context, Receiver<Object> out) {
        state.put(context, new HashMap<>());
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        state.act(context, latches -> {
            Object key = keyStash == null ? null : context.fetch(keyStash).findFirst().orElseThrow();
            Object value = context.fetch(valueStash).findFirst().orElseThrow();
            if (!value.equals(latches.get(key))) {
                latches.put(key, value);
                out.accept(context, in);
            }
        });
    }

    @Override
    public void finish(Context context, Receiver<Object> out) {
        state.remove(context);
    }

    public static class Parameters {

        public String key;
        public String value;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String value) {
            this.value = value;
        }
    }
}

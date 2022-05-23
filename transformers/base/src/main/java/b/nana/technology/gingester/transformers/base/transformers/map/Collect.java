package b.nana.technology.gingester.transformers.base.transformers.map;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMapReduce;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.HashMap;
import java.util.Map;

public final class Collect implements Transformer<Object, Map<Object, Object>> {

    private final ContextMapReduce<Map<Object, Object>> maps = new ContextMapReduce<>();

    private final FetchKey fetchValue;
    private final boolean throwOnCollision;

    public Collect(Parameters parameters) {
        fetchValue = parameters.value;
        throwOnCollision = parameters.throwOnCollision;
    }

    @Override
    public void prepare(Context context, Receiver<Map<Object, Object>> out) {
        maps.put(context, HashMap::new);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Map<Object, Object>> out) throws Exception {
        Object value = context.require(fetchValue);
        Map<Object, Object> map = maps.get(context);
        Object collision = map.put(in, value);
        if (collision != null && throwOnCollision) {
            throw new IllegalStateException("Key already present: " + in);
        }
    }

    @Override
    public void finish(Context context, Receiver<Map<Object, Object>> out) {
        out.accept(context, maps.remove(context)
                .reduce((a, b) -> {
                    if (a.size() > b.size()) {
                        a.putAll(b);
                        return a;
                    } else {
                        b.putAll(a);
                        return b;
                    }
                })
                .orElseThrow());
    }

    public static class Parameters {

        public FetchKey value = new FetchKey(1);
        public boolean throwOnCollision = false;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(FetchKey value) {
            this.value = value;
        }
    }
}

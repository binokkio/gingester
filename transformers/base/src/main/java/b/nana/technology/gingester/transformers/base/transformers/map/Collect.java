package b.nana.technology.gingester.transformers.base.transformers.map;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMapReduce;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformers.Fetch;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

public final class Collect implements Transformer<Object, Map<Object, Object>> {

    private final ContextMapReduce<Map<Object, Object>> maps = new ContextMapReduce<>();

    private final String[] valueStashName;
    private final boolean throwOnCollision;

    public Collect(Parameters parameters) {
        valueStashName = Fetch.parseStashName(parameters.value);
        throwOnCollision = parameters.throwOnCollision;
    }

    @Override
    public void prepare(Context context, Receiver<Map<Object, Object>> out) {
        maps.put(context, HashMap::new);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Map<Object, Object>> out) throws Exception {
        Optional<Object> value = context.fetch(valueStashName).findFirst();
        if (value.isPresent()) {
            Map<Object, Object> map = maps.get(context);
            Object collision = map.put(in, value.get());
            if (collision != null && throwOnCollision) {
                throw new IllegalStateException("Key already present: " + in);
            }
        } else {
            throw new NoSuchElementException("Empty value fetch: " + String.join(".", valueStashName));
        }
    }

    @Override
    public void finish(Context context, Receiver<Map<Object, Object>> out) {

        Map<Object, Object> index = maps.remove(context)
                .reduce((a, b) -> {
                    if (a.size() > b.size()) {
                        a.putAll(b);
                        return a;
                    } else {
                        b.putAll(a);
                        return b;
                    }
                })
                .orElseThrow();

        out.accept(context, index);
    }

    public static class Parameters {

        public String value = "";
        public boolean throwOnCollision = false;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String value) {
            this.value = value;
        }
    }
}

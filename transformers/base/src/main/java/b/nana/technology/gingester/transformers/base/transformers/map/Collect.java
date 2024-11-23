package b.nana.technology.gingester.transformers.base.transformers.map;

import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;
import java.util.Objects;

@Example(example = "", description = "Collect a map of item value -> most recently stashed value")
@Example(example = "hello", description = "Collect a map of item value -> fetch `hello`")
public final class Collect implements Transformer<Object, Map<Object, Object>> {

    private final ContextMap<Map<Object, Object>> maps = new ContextMap<>();

    private final FetchKey fetchValue;
    private final MapType mapType;
    private final boolean throwOnCollision;

    public Collect(Parameters parameters) {
        fetchValue = parameters.value;
        mapType = parameters.mapType;
        throwOnCollision = parameters.throwOnCollision;
    }

    @Override
    public void prepare(Context context, Receiver<Map<Object, Object>> out) {
        maps.put(context, mapType.newMap());
    }

    @Override
    public void transform(Context context, Object in, Receiver<Map<Object, Object>> out) throws Exception {
        Object value = context.require(fetchValue);
        maps.act(context, map ->
            map.compute(in, (key, collision) -> {
                if (throwOnCollision && collision != null && !Objects.equals(in, collision))
                    throw new IllegalStateException("Collision for key: " + in);
                return value;
            })
        );
    }

    @Override
    public void finish(Context context, Receiver<Map<Object, Object>> out) {
        out.accept(context, maps.remove(context));
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order({ "value", "mapType" })
    public static class Parameters {
        public FetchKey value = new FetchKey(1);
        public MapType mapType = MapType.HASH_MAP;
        public boolean throwOnCollision = true;
    }
}

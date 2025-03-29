package b.nana.technology.gingester.transformers.base.transformers.set;

import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Set;

public final class Collect implements Transformer<Object, Set<?>> {

    private final ContextMap<Set<Object>> contextMap = new ContextMap<>();
    private final SetType setType;

    public Collect(Parameters parameters) {
        setType = parameters.type;
    }

    @Override
    public void prepare(Context context, Receiver<Set<?>> out) {
        contextMap.put(context, setType.newSet());
    }

    @Override
    public void transform(Context context, Object in, Receiver<Set<?>> out) throws Exception {
        contextMap.act(context, list -> list.add(in));
    }

    @Override
    public void finish(Context context, Receiver<Set<?>> out) {
        out.accept(context, contextMap.remove(context));
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order("type")
    public static class Parameters {
        public SetType type = SetType.HASH_SET;
    }
}

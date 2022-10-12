package b.nana.technology.gingester.transformers.base.transformers.set;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Set;
import java.util.function.Supplier;

public final class Collect implements Transformer<Object, Set<?>> {

    private final ContextMap<Set<Object>> contextMap = new ContextMap<>();
    private final Supplier<Set<Object>> setSupplier;

    public Collect(Parameters parameters) {
        setSupplier = parameters.type.setSupplier;
    }

    @Override
    public void prepare(Context context, Receiver<Set<?>> out) {
        contextMap.put(context, setSupplier.get());
    }

    @Override
    public void transform(Context context, Object in, Receiver<Set<?>> out) throws Exception {
        contextMap.act(context, list -> list.add(in));
    }

    @Override
    public void finish(Context context, Receiver<Set<?>> out) {
        out.accept(context, contextMap.remove(context));
    }

    public static class Parameters {

        public Type type = Type.HASH_SET;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(Type type) {
            this.type = type;
        }
    }
}

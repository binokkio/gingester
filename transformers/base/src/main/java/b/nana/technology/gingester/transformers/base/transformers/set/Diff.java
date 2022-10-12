package b.nana.technology.gingester.transformers.base.transformers.set;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class Diff implements Transformer<Set<?>, Set<?>> {

    private final ContextMap<State> states = new ContextMap<>();
    private final Supplier<Set<Object>> setSupplier;

    public Diff(Parameters parameters) {
        setSupplier = parameters.type.setSupplier;
    }

    @Override
    public Map<String, Object> getStashDetails() {
        return Map.of(
                "added", Set.class,
                "removed", Set.class
        );
    }

    @Override
    public void prepare(Context context, Receiver<Set<?>> out) {
        states.put(context, new State());
    }

    @Override
    public void transform(Context context, Set<?> in, Receiver<Set<?>> out) throws Exception {

        Set<?> previous = states.apply(context, state -> {
            Set<?> result = state.previous;
            state.previous = in;
            return result;
        });

        Set<Object> added = setSupplier.get();
        added.addAll(in);
        added.removeAll(previous);

        Set<Object> removed = setSupplier.get();
        removed.addAll(previous);
        removed.removeAll(in);

        out.accept(
                context.stash(Map.of(
                        "added", added,
                        "removed", removed
                )),
                in
        );
    }

    @Override
    public void finish(Context context, Receiver<Set<?>> out) {
        states.remove(context);
    }

    private static class State {
        Set<?> previous = Set.of();
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

package b.nana.technology.gingester.transformers.base.transformers.set;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.StashDetails;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;
import java.util.Set;

public final class Diff implements Transformer<Set<?>, Set<?>> {

    private final ContextMap<State> states = new ContextMap<>();
    private final SetType setType;

    public Diff(Parameters parameters) {
        setType = parameters.type;
    }

    @Override
    public StashDetails getStashDetails() {
        return StashDetails.of(
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

        Set<Object> added = setType.newSet();
        added.addAll(in);
        added.removeAll(previous);

        Set<Object> removed = setType.newSet();
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

        public SetType type = SetType.HASH_SET;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(SetType type) {
            this.type = type;
        }
    }
}

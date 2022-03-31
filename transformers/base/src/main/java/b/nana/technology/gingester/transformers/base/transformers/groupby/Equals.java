package b.nana.technology.gingester.transformers.base.transformers.groupby;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Passthrough
public final class Equals implements Transformer<Object, Object> {

    private final ContextMap<State> contextMap = new ContextMap<>();

    @Override
    public void setup(SetupControls controls) {
        controls.syncs(List.of("__seed__"));
    }

    @Override
    public void prepare(Context context, Receiver<Object> out) {
        contextMap.put(context, new State(context, out));
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        Context group = contextMap.apply(context, state -> state.getGroup(in));
        out.accept(context.extend().group(group), in);
    }

    @Override
    public void finish(Context context, Receiver<Object> out) {
        contextMap.remove(context).close();
    }

    public static class Parameters {

        public long limit;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(long limit) {
            this.limit = limit;
        }
    }

    private static class State {

        private final Context groupParent;
        private final Receiver<Object> out;
        private final Map<Object, Context> groups = new HashMap<>();

        public State(Context groupParent, Receiver<Object> out) {
            this.groupParent = groupParent;
            this.out = out;
        }

        private Context getGroup(Object object) {
            return groups.computeIfAbsent(object, o -> out.acceptGroup(groupParent.stash("groupKey", o)));
        }

        private void close() {
            groups.values().forEach(out::closeGroup);
        }
    }
}

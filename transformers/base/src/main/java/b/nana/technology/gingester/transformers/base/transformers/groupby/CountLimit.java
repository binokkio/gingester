package b.nana.technology.gingester.transformers.base.transformers.groupby;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.List;

@Passthrough
public final class CountLimit implements Transformer<Object, Object> {

    private final ContextMap<State> contextMap = new ContextMap<>();
    private final long limit;

    public CountLimit(Parameters parameters) {
        limit = parameters.limit;
    }

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

        Context.Builder contextBuilder = contextMap.act(context, state -> {
            return state.group(context);
        });

        out.accept(contextBuilder, in);
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

    private class State {

        private final Context groupParent;
        private final Receiver<Object> out;

        private Context group;
        private long counter;

        public State(Context groupParent, Receiver<Object> out) {
            this.groupParent = groupParent;
            this.out = out;
        }

        private Context.Builder group(Context context) {

            long count = counter++;

            if (count % limit == 0) {
                close();
                group = out.acceptGroup(groupParent.stash("limitGroup", counter / limit));
            }

            return context
                    .stash("count", count)
                    .group(group);
        }

        private void close() {
            if (group != null) {
                out.closeGroup(group);
            }
        }
    }
}

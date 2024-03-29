package b.nana.technology.gingester.transformers.base.transformers.groupby;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

@Passthrough
public final class CountLimit implements Transformer<Object, Object> {

    private final ContextMap<State> contextMap = new ContextMap<>();
    private final long limit;

    public CountLimit(Parameters parameters) {
        limit = parameters.limit;
    }

    @Override
    public void prepare(Context context, Receiver<Object> out) {
        contextMap.put(context, new State(context, out));
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {

        /*
            For CountLimit groups are created and closed in the `group` call, so we need
            to `out.accept` within the `contextMap.act`. What would not work is using
            `contextMap.apply` to get the group and then `out.accept` after that because
            an interleaving `transform` might call `group` and close the group between the
            `contextMap.apply` and `out.accept`.
         */

        contextMap.act(context, state -> out.accept(state.group(context), in));
    }

    @Override
    public void finish(Context context, Receiver<Object> out) {
        contextMap.remove(context).close();
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

    public static class Parameters {

        public long limit;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(long limit) {
            this.limit = limit;
        }
    }
}

package b.nana.technology.gingester.transformers.base.transformers.groupby;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Passthrough
public final class CountModulo implements Transformer<Object, Object> {

    private final ContextMap<State> contextMap = new ContextMap<>();
    private final int divisor;

    public CountModulo(Parameters parameters) {
        divisor = parameters.divisor;
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

        /*
            For CountModulo all groups are created in `prepare` and closed in `finish`
            so there is no need to synchronize within the state at all.
         */

        Context.Builder contextBuilder = contextMap.get(context).group(context);
        out.accept(contextBuilder, in);
    }

    @Override
    public void finish(Context context, Receiver<Object> out) {
        contextMap.remove(context).close(out);
    }

    public static class Parameters {

        public int divisor;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(int divisor) {
            this.divisor = divisor;
        }
    }

    private class State {

        private final AtomicLong counter = new AtomicLong();

        private final Context[] groups;

        private State(Context groupParent, Receiver<Object> out) {
            groups = new Context[divisor];
            for (int i = 0; i < groups.length; i++) {
                groups[i] = out.acceptGroup(groupParent.stash("countModulo", i));
            }
        }

        private Context.Builder group(Context context) {
            long count = counter.getAndIncrement();
            return context
                    .stash("count", count)
                    .group(groups[(int) (count % divisor)]);
        }

        private void close(Receiver<Object> out) {
            for (Context group : groups) {
                out.closeGroup(group);
            }
        }
    }
}

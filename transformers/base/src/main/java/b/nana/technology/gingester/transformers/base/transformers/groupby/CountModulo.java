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
        contextMap.put(context, new State(context, out, divisor));
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {

        Context.Builder contextBuilder = contextMap.act(context, state -> {
            long count = state.counter++;
            return context
                    .stash("count", count)
                    .group(state.groups[(int) (count % divisor)]);
        });

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

    private static class State {

        private final Context[] groups;
        private long counter;

        private State(Context context, Receiver<Object> out, int divisor) {
            groups = new Context[divisor];
            for (int i = 0; i < groups.length; i++) {
                groups[i] = out.acceptGroup(context.stash("countModulo", i));
            }
        }

        private void close(Receiver<Object> out) {
            for (Context group : groups) {
                out.closeGroup(group);
            }
        }
    }
}

package b.nana.technology.gingester.core.transformers.groupby;

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

    private final int divisor;
    private final ContextMap<State> contextMap = new ContextMap<>();

    public CountModulo(Parameters parameters) {
        divisor = parameters.divisor;
    }

    @Override
    public void setup(SetupControls controls) {
        controls.syncs(List.of("__seed__"));
    }

    @Override
    public void prepare(Context context, Receiver<Object> out) throws Exception {
        contextMap.put(context, new State(context, out, divisor));
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {

        Context group = contextMap.act(context, state ->
                state.groups[(int) (state.counter++ % divisor)]);

        out.accept(context.extend().group(group), in);
    }

    @Override
    public void finish(Context context, Receiver<Object> out) throws Exception {
        State state = contextMap.remove(context);
        for (Context group : state.groups) {
            out.closeGroup(group);
        }
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

        private long counter;
        private final Context[] groups;

        private State(Context context, Receiver<Object> out, int divisor) {
            groups = new Context[divisor];
            for (int i = 0; i < groups.length; i++) {
                groups[i] = out.acceptGroup(new Context.Builder(context).stash("countModulo", i));
            }
        }
    }
}

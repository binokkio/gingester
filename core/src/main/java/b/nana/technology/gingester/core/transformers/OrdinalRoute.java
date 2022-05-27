package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.ArrayList;
import java.util.List;

@Passthrough
public final class OrdinalRoute implements Transformer<Object, Object> {

    private final ContextMap<Counter> counters = new ContextMap<>();
    private final List<String> routes;

    public OrdinalRoute(Parameters parameters) {
        routes = parameters;
    }

    @Override
    public void setup(SetupControls controls) {
        controls.links(routes);
    }

    @Override
    public void prepare(Context context, Receiver<Object> out) throws Exception {
        counters.put(context, new Counter());
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        int count = counters.apply(context, Counter::getAndIncrement);
        if (count >= routes.size()) throw new IllegalStateException("No route for ordinal " + count);
        out.accept(context, in, routes.get(count));
    }

    @Override
    public void finish(Context context, Receiver<Object> out) throws Exception {
        counters.remove(context);
    }

    public static class Parameters extends ArrayList<String> {

    }

    private static class Counter {

        private int count;

        public int getAndIncrement() {
            return count++;
        }
    }
}

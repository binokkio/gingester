package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Description;
import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.ArrayList;
import java.util.List;

@Names(1)
@Passthrough
@Description("Route items based on their ordinality within the sync context")
@Example(example = "A B", description = "Route the first item to A, the second to B, throw if there is a third")
public class OrdinalRoute implements Transformer<Object, Object> {

    private final ContextMap<Counter> counters = new ContextMap<>();
    private final List<String> routes;
    private final boolean cycle;
    // TODO add latch, meaning to keep using the last route if count > routes.size()

    public OrdinalRoute(Parameters parameters) {
        this(parameters, false);
    }

    public OrdinalRoute(List<String> routes, boolean cycle) {
        this.routes = routes;
        this.cycle = cycle;
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
        String link;
        if (cycle) {
            link = routes.get(count % routes.size());
        } else {
            if (count >= routes.size()) throw new IllegalStateException("No route for ordinal " + count);
            link = routes.get(count);
        }
        out.accept(context, in, link);
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

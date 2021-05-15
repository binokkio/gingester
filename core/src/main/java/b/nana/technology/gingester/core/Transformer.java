package b.nana.technology.gingester.core;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public abstract class Transformer<I, O> {

    final Class<I> inputClass;
    final Class<O> outputClass;
    final List<Link<? extends I>> inputs = new ArrayList<>();
    final List<Link<O>> outputs = new ArrayList<>();
    final List<Transformer<?, ?>> syncs = new ArrayList<>();
    final Object parameters;

    protected Transformer() {
        this(null);
    }

    @SuppressWarnings("unchecked")
    protected Transformer(Object parameters) {
        // TODO don't assume "we" are the immediate super class, traverse up the hierarchy until we find ourselves
        Type[] actualTypeArguments = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments();
        inputClass = (Class<I>) actualTypeArguments[0];
        outputClass = (Class<O>) actualTypeArguments[1];
        this.parameters = parameters;
    }

    Transformer(Class<I> inputClass, Class<O> outputClass) {
        this.inputClass = inputClass;
        this.outputClass = outputClass;
        this.parameters = null;
    }

    void setup() {
        setup(new Setup());
    }

    /**
     * Called after all transformers been linked.
     * @param setup
     */
    protected void setup(Setup setup) {

    }

    /**
     * Can be called concurrently!
     */
    protected abstract void transform(Context context, I input) throws Exception;

    /**
     * Called when no more input will come for the given context.
     *
     * Will only be called for contexts from upstream transformers that are synced
     * with this transformer, i.e. those for which {@link Gingester#sync(Transformer, Transformer)} sync}
     * was called with the upstream transformer as first and this transformer as second argument.
     */
    protected void finish(Context context) {

    }

    protected final void emit(Context.Builder context, O output) {
        emit(context.build(), output);
    }

    protected final void emit(Context context, O output) {
        for (int i = 0; i < outputs.size(); i++) {
            emit(context, output, i);
        }
    }

    protected final void emit(Context.Builder context, O output, int direction) {
        emit(context.build(), output, direction);
    }

    protected final void emit(Context context, O output, int direction) {
        Worker worker = (Worker) Thread.currentThread();
        worker.accept(this, context, output, direction);
    }

    protected final void recurse(Context.Builder contextBuilder, I value) throws Exception {
        Context context = contextBuilder.build();
        transform(context, value);
        for (Transformer<?, ?> sync : syncs) {
            sync.finish(context);
        }
    }

    /**
     * @return list of all downstream (partial and full) routes
     */
    List<Deque<Link<?>>> getDownstreamRoutes() {

        List<Deque<Link<?>>> routes = new ArrayList<>();
        for (Link<?> link : outputs) {
            Deque<Link<?>> route = new ArrayDeque<>();
            route.add(link);
            routes.add(route);
        }

        List<Deque<Link<?>>> discovered = routes;
        while (!discovered.isEmpty()) {
            discovered = discovered.stream()
                    .flatMap(route -> route.getLast().to.outputs.stream()
                            .filter(link -> !route.contains(link))
                            .map(link -> {
                                Deque<Link<?>> copy = new ArrayDeque<>(route);
                                copy.add(link);
                                return copy;
                            }))
                    .collect(Collectors.toList());
            routes.addAll(discovered);
        }

        return routes;
    }

    public class Setup {

        public void syncInputs() {
            inputs.forEach(Link::sync);
        }

        public void syncOutputs() {
            outputs.forEach(Link::sync);
        }

        public void assertNoInputs() {
            if (inputs.stream().anyMatch(link -> link.from != null)) {
                throw new IllegalStateException("inputs");  // TODO
            }
        }

        public void maxBatchSize(int maxBatchSize) {
            // TODO
        }
    }
}

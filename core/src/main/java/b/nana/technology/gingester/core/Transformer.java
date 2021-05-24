package b.nana.technology.gingester.core;

import net.jodah.typetools.TypeResolver;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Transformer<I, O> {

    Gingester gingester;
    String name;
    final Object parameters;
    final Class<I> inputClass;
    final Class<O> outputClass;
    final List<Link<? extends I>> incoming = new ArrayList<>();
    final List<Link<O>> outgoing = new ArrayList<>();
    final List<Transformer<?, ?>> syncs = new ArrayList<>();
    final BlockingQueue<Batch<? extends I>> queue = new ArrayBlockingQueue<>(100);
    final Set<Worker.Transform> workers = new HashSet<>();
    private final Threader threader = new Threader();
    private int state = Integer.MAX_VALUE;
    int maxBatchSize = 65536;
    volatile int batchSize = 1;

    protected Transformer() {
        this(null);
    }

    @SuppressWarnings("unchecked")
    protected Transformer(Object parameters) {
        Class<?>[] types = TypeResolver.resolveRawArguments(Transformer.class, this.getClass());
        this.inputClass = (Class<I>) types[0];
        this.outputClass = (Class<O>) types[1];
        this.parameters = parameters;
    }

    Transformer(Class<I> inputClass, Class<O> outputClass) {
        this.inputClass = inputClass;
        this.outputClass = outputClass;
        this.parameters = null;
    }

    void setName(String name) {
        this.name = name;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    List<Class<?>> getInputClasses() {
        return List.of(inputClass);
    }

    List<Class<?>> getOutputClasses() {
        return List.of(outputClass);
    }

    void assertCanLinkTo(Transformer<?, ?> to) {

        if (to == this || to.getDownstream().contains(this)) {
            throw new IllegalStateException(String.format(
                    "Linking from %s to %s would create a circular route",
                    getName().orElseGet(() -> Provider.name(this)),
                    to.getName().orElseGet(() -> Provider.name(to))
            ));
        }

        for (Class<?> outputClass : getOutputClasses()) {
            for (Class<?> inputClass : to.getInputClasses()) {
                if (!inputClass.isAssignableFrom(outputClass)) {
                    throw new IllegalStateException(String.format(
                            "Can't link from %s to %s, %s can not be assigned to %s",
                            getName().orElseGet(() -> Provider.name(this)),
                            to.getName().orElseGet(() -> Provider.name(to)),
                            outputClass.getCanonicalName(),
                            inputClass.getCanonicalName()
                    ));
                }
            }
        }
    }

    void apply(Configuration.TransformerConfiguration configuration) {
        if (configuration.maxWorkers != null) {
            state = Math.min(state, configuration.maxWorkers);
        }
    }

    void setup(Gingester gingester) {
        this.gingester = gingester;
        setup(new Setup());
    }

    void put(Batch<? extends I> batch) throws InterruptedException {
        queue.put(batch);
        gingester.signalNewBatch(this);
    }

    boolean isEmpty() {
        return queue.isEmpty() && workers.isEmpty();
    }

    boolean isOpen() {
        return state > 0;
    }

    boolean isClosed() {
        return state == -1;
    }

    void setIsClosing() {
        state = 0;
    }

    void setIsClosed() {
        state = -1;
    }



    // methods to be overridden by subclasses

    /**
     * Called after all transformers been linked.
     */
    protected void setup(Setup setup) {}

    /**
     * Called before the first input arrives for this transformer.
     */
    protected void open() throws Exception {}

    /**
     * Called before the first input arrives for the given context.
     */
    protected void prepare(Context context) throws Exception {}

    /**
     * Can be called concurrently!
     */
    protected abstract void transform(Context context, I input) throws Exception;

    /**
     * Called when no more input will come for the given context.
     *
     * Will only be called for contexts from upstream transformers that are synced
     * with this transformer, i.e. those for which {@link Gingester.Builder#sync(Transformer, Transformer)} sync}
     * was called with the upstream transformer as first and this transformer as second argument.
     */
    protected void finish(Context context) throws Exception {}

    /**
     * Called when no more input will come for this transformer.
     */
    protected void close() throws Exception {}



    // methods available to subclasses

    protected final void emit(Context.Builder context, O output) {
        emit(context.build(), output);
    }

    protected final void emit(Context context, O output) {
        for (int i = 0; i < outgoing.size(); i++) {
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

    protected final <T extends I> void recurse(Context.Builder contextBuilder, T value) {
        Context context = contextBuilder.build();
        Worker.prepare(this, context);
        Worker.transform(this, context, value);
        Worker.finish(this, context);
    }

    protected final void ack() {
        getStatistics().ifPresent(statistics -> statistics.acks.incrementAndGet());
    }

    protected final void ack(long acks) {
        getStatistics().ifPresent(statistics -> statistics.acks.addAndGet(acks));
    }

    protected final Threader getThreader() {
        return threader;
    }



    // upstream and downstream discovery

    Set<Transformer<?, ?>> getUpstream() {
        return getUpstreamRoutes().stream()
                .flatMap(Collection::stream)
                .filter(transformer -> !transformer.equals(this))
                .collect(Collectors.toSet());
    }

    Set<Transformer<?, ?>> getDownstream() {
        return getDownstreamRoutes().stream()
                .flatMap(Collection::stream)
                .filter(transformer -> !transformer.equals(this))
                .collect(Collectors.toSet());
    }

    List<ArrayDeque<Transformer<?, ?>>> getUpstreamRoutes() {
        return getRoutes(transformer -> transformer.incoming.stream().map(link -> link.from));
    }

    List<ArrayDeque<Transformer<?, ?>>> getDownstreamRoutes() {
        return getRoutes(transformer -> transformer.outgoing.stream().map(link -> link.to));
    }

    /**
     * All partial and complete routes from `this` transformer up- or downstream depending on the given stepper.
     */
    private List<ArrayDeque<Transformer<?, ?>>> getRoutes(Function<Transformer<?, ?>, Stream<Transformer<?, ?>>> stepper) {

        ArrayDeque<Transformer<?, ?>> start = new ArrayDeque<>();
        start.add(this);

        List<ArrayDeque<Transformer<?, ?>>> routes = new ArrayList<>();
        routes.add(start);

        List<ArrayDeque<Transformer<?, ?>>> discovered = routes;
        while (!discovered.isEmpty()) {
            discovered = discovered.stream()
                    .flatMap(route -> stepper
                            .apply(route.getLast())
                            .map(transformer -> {
                                ArrayDeque<Transformer<?, ?>> copy = new ArrayDeque<>(route);
                                copy.add(transformer);
                                return copy;
                            }))
                    .collect(Collectors.toList());
            routes.addAll(discovered);
        }

        return routes;
    }



    //

    public class Setup {

        public void preferUpstreamSync() {
            incoming.forEach(Link::preferSync);
        }

        public void preferDownstreamSync() {
            outgoing.forEach(Link::preferSync);
        }

        public void requireUpstreamSync() {
            incoming.forEach(Link::requireSync);
        }

        public void requireDownstreamSync() {
            outgoing.forEach(Link::requireSync);
        }

        public void requireUpstreamAsync() {
            incoming.forEach(Link::requireAsync);
        }

        public void requireDownstreamAsync() {
            outgoing.forEach(Link::requireAsync);
        }

        public void assertNoUpstream() {
            if (!incoming.isEmpty()) {
                throw new IllegalStateException("upstream");  // TODO
            }
        }

        public void assertNoDownstream() {
            if (!outgoing.isEmpty()) {
                throw new IllegalStateException("downstream");  // TODO
            }
        }

        public void limitBatchSize(int limit) {
            maxBatchSize = Math.min(maxBatchSize, limit);
        }

        public void limitWorkers(int limit) {
            state = Math.min(state, limit);
        }
    }

    public class Threader {

        // TODO use a thread pool

        public final Thread newThread(Runnable runnable) {
            return new Worker.Jobs(gingester, Transformer.this, runnable::run);
        }

        public void execute(Runnable runnable) {
            newThread(runnable).start();
        }

        public void schedule(Runnable runnable, long l, TimeUnit timeUnit) {
            gingester.scheduler.schedule(() -> execute(runnable), l, timeUnit);
        }

        public void scheduleAtFixedRate(Runnable runnable, long l, long l1, TimeUnit timeUnit) {
            gingester.scheduler.scheduleAtFixedRate(() -> execute(runnable), l, l1, timeUnit);
        }

        public void scheduleWithFixedDelay(Runnable runnable, long l, long l1, TimeUnit timeUnit) {
            gingester.scheduler.scheduleWithFixedDelay(() -> execute(runnable), l, l1, timeUnit);
        }
    }



    // statistics

    private Statistics statistics;

    void enableStatistics() {
        this.statistics = new Statistics();
    }

    Optional<Statistics> getStatistics() {
        return Optional.ofNullable(statistics);
    }

    static class Statistics {

        final AtomicLong delt = new AtomicLong();
        final Sampler deltSampler = new Sampler(delt::get);

        final AtomicLong acks = new AtomicLong();
        final Sampler acksSampler = new Sampler(acks::get);

        void sample() {
            deltSampler.sample();
            acksSampler.sample();
        }

        @Override
        public String toString() {

            StringBuilder stringBuilder = new StringBuilder(String.format(
                    "%,d processed",
                    delt.get()
            ));

            long acks = this.acks.get();
            if (acks != 0) {
                stringBuilder.append(String.format(
                        ", %,d acknowledged at %,.2f/s",
                        acks,
                        acksSampler.getChangePerSecond()
                ));
            } else {
                stringBuilder.append(String.format(
                        " at %,.2f/s",
                        deltSampler.getChangePerSecond()
                ));
            }

            return stringBuilder.toString();
        }
    }
}

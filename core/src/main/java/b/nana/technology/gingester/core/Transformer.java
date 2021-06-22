package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.link.BaseLink;
import b.nana.technology.gingester.core.link.ExceptionLink;
import b.nana.technology.gingester.core.link.NormalLink;
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
    Object parameters;
    final Class<I> inputClass;
    final Class<O> outputClass;
    final List<BaseLink<?, ? extends I>> incoming = new ArrayList<>();
    final List<NormalLink<O>> outgoing = new ArrayList<>();
    final List<Transformer<?, ?>> syncs = new ArrayList<>();
    final List<ExceptionLink> excepts = new ArrayList<>();
    final Map<String, NormalLink<O>> outgoingByName = new HashMap<>();
    final BlockingQueue<Batch<? extends I>> queue = new ArrayBlockingQueue<>(100);
    final Set<Worker.Transform> workers = new HashSet<>();
    private final Threader threader = new Threader();
    private int state = 1;
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

    public final Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    List<Class<?>> getInputClasses() {
        return List.of(inputClass);
    }

    List<Class<?>> getOutputClasses() {
        return List.of(outputClass);
    }

    void assertLinkToWouldNotBeCircular(Transformer<?, ?> to) {
        if (to == this || to.getDownstream().contains(this)) {
            throw new IllegalStateException(String.format(
                    "Linking from %s to %s would create a circular route",
                    getName().orElseGet(() -> Provider.name(this)),
                    to.getName().orElseGet(() -> Provider.name(to))
            ));
        }
    }

    void assertLinkToWouldBeCompatible(Transformer<?, ?> to) {

        // don't check Fetch for now, will throw a ClassCastException at Runtime when incorrectly linked
        // TODO implement Fetch assertCanLinkTo check
        if (getClass().equals(Fetch.class)) {
            return;
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
        if (configuration.workers != null) {
            state = configuration.workers;
        }
    }

    void setup(Gingester gingester) {
        this.gingester = gingester;
        outgoing.forEach(link -> outgoingByName.put(link.to.getName().orElseThrow(), link));  // TODO bit out of place
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

    public int getMaxWorkers() {
        return state;
    }

    // methods to be overridden by subclasses

    /**
     * TODO
     */
    public List<String> getLinks() {
        return Collections.emptyList();
    }

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
     * with this transformer, i.e. those for which {@link Builder#sync(Transformer, Transformer)} sync}
     * was called with the upstream transformer as first and this transformer as second argument.
     */
    protected void finish(Context context) throws Exception {}

    /**
     * Called when no more input will come for this transformer.
     */
    protected void close() throws Exception {}



    // methods available to (some) subclasses

    final void emitUnchecked(Context.Builder context, Object output) {
        _emit(context.build(), check(output), outgoing);
    }

    final void emitUnchecked(Context context, Object output) {
        _emit(maybeExtend(context), check(output), outgoing);
    }

    protected final void emit(Context.Builder context, O output) {
        _emit(context.build(), output, outgoing);
    }

    protected final void emit(Context context, O output) {
        _emit(maybeExtend(context), output, outgoing);
    }

    protected final void emit(Context.Builder context, O output, String direction) {
        _emit(context.build(), output, List.of(outgoingByName.get(direction)));
    }

    protected final void emit(Context context, O output, String direction) {
        _emit(maybeExtend(context), output, List.of(outgoingByName.get(direction)));
    }

    protected final void emit(Context.Builder context, O output, List<String> directions) {
        _emit(context.build(), output, directions.stream().map(outgoingByName::get).collect(Collectors.toList()));
    }

    protected final void emit(Context context, O output, List<String> directions) {
        _emit(maybeExtend(context), output, directions.stream().map(outgoingByName::get).collect(Collectors.toList()));
    }

    private void _emit(Context context, O output, List<NormalLink<O>> directions) {
        Worker worker = (Worker) Thread.currentThread();
        worker.accept(this, context, output, directions);
    }

    @SuppressWarnings("unchecked")  // checked at runtime
    private O check(Object output) {
        for (Class<?> outputClass : getOutputClasses()) {
            if (!outputClass.isAssignableFrom(output.getClass())) {
                throw new IllegalStateException("Incompatible output");  // TODO
            }
        }
        return (O) output;
    }

    private Context maybeExtend(Context context) {
        if ((!excepts.isEmpty() || !syncs.isEmpty()) && context.transformer != this) {
            return context.extend(this).build();
        } else {
            return context;
        }
    }

    protected final <T extends I> void recurse(Context.Builder contextBuilder, T value) {
        Context context = contextBuilder.build();
        Worker worker = (Worker) Thread.currentThread();
        worker.prepare(this, context);
        worker.transform(this, context, value);
        worker.finish(this, context);
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

    private static final Function<Transformer<?, ?>, Stream<Transformer<?, ?>>> UPSTREAM_STEPPER =
            transformer -> transformer.getIncoming().stream().map(link -> link.from);

    private static final Function<Transformer<?, ?>, Stream<Transformer<?, ?>>> DOWNSTREAM_STEPPER =
            transformer -> transformer.getOutgoing().stream().map(link -> link.to);

    List<BaseLink<?, ? extends I>> getIncoming() {
        return incoming;
    }

    List<BaseLink<?, ?>> getOutgoing() {
        List<BaseLink<?, ?>> result = new ArrayList<>(outgoing);
        result.addAll(excepts);
        return result;
    }

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
        return getRoutes(UPSTREAM_STEPPER);
    }

    List<ArrayDeque<Transformer<?, ?>>> getDownstreamRoutes() {
        return getRoutes(DOWNSTREAM_STEPPER);
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

        public void preferUpstreamAsync() {
            incoming.forEach(BaseLink::preferAsync);
        }

        public void preferDownstreamAsync() {
            outgoing.forEach(BaseLink::preferAsync);
        }

        public void requireUpstreamSync() {
            incoming.forEach(BaseLink::requireSync);
        }

        public void requireDownstreamSync() {
            outgoing.forEach(BaseLink::requireSync);
        }

        public void requireUpstreamAsync() {
            incoming.forEach(BaseLink::requireAsync);
        }

        public void requireDownstreamAsync() {
            outgoing.forEach(BaseLink::requireAsync);
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

        public int getDirection(String transformerName) {
            for (int i = 0; i < outgoing.size(); i++) {
                if (outgoing.get(i).to.name.equals(transformerName)) {
                    return i;
                }
            }
            throw new IllegalStateException("No link to transformer named " + transformerName);
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

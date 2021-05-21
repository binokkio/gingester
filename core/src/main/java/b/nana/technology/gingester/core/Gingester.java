package b.nana.technology.gingester.core;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class Gingester {

    enum State {
        SETUP,
        RUNNING,
        DONE
    }

    private final Set<Transformer<?, ?>> transformers = new LinkedHashSet<>();
    private final Map<String, Transformer<?, ?>> transformerMap = new HashMap<>();
    private final Set<Worker.Transform> seeders = new HashSet<>();
    private final Set<Worker.Transform> workers = new HashSet<>();
    private final BlockingQueue<Runnable> signals = new LinkedBlockingQueue<>();

    State state = State.SETUP;

    private void addTransformer(Transformer<?, ?> transformer) {

        if (transformer.gingester != null && transformer.gingester != this) {
            throw new IllegalArgumentException("Transformers can not be associated with multiple Gingesters");
        }

        transformer.gingester = this;
        transformers.add(transformer);
    }

    Set<Transformer<?, ?>> getTransformers() {
        return transformers;
    }

    /**
     * Allow the given transformer to be referenced by the given name.
     *
     * The name must not have been given to any other transformer.
     * A transformer may only be given 1 name.
     */
    public void name(String name, Transformer<?, ?> transformer) {
        if (transformerMap.containsValue(transformer)) throw new IllegalArgumentException("Transformer was already named");
        addTransformer(transformer);
        Transformer<?, ?> collision = transformerMap.put(name, transformer);
        if (collision != null) throw new IllegalArgumentException("Transformer name not unique: " + name);
    }

    public Optional<String> getName(Transformer<?, ?> transformer) {
        return transformerMap.entrySet().stream()
                .filter(e -> e.getValue() == transformer)
                .findFirst()
                .map(Map.Entry::getKey);
    }

    public Transformer<?, ?> getTransformer(String name) {
        Transformer<?, ?> transformer = transformerMap.get(name);
        if (transformer != null) return transformer;
        return transformers.stream()
                .filter(t -> Provider.name(t).equals(name))
                .reduce((a, b) -> { throw new IllegalStateException("Multiple matches for " + name); })
                .orElseThrow(() -> new NoSuchElementException("No transformer named " + name));
    }

    @SuppressWarnings("unchecked")  // checked at runtime
    public <T extends Transformer<?, ?>> T getTransformer(String name, Class<T> transformerClass) {
        T transformer = (T) transformerMap.get(name);
        if (transformer == null) throw new NoSuchElementException("No transformer named " + name);
        if (!transformerClass.isInstance(transformer)) throw new ClassCastException();  // TODO
        return transformer;
    }

    @SuppressWarnings("unchecked")  // checked at runtime in link()
    public <T> Link<T> link(String fromName, String toName) {
        Transformer<?, T> from = (Transformer<?, T>) getTransformer(fromName);
        Transformer<? super T, ?> to = (Transformer<? super T, ?>) getTransformer(toName);
        return link(from, to);
    }

    public <T> Link<T> link(Transformer<?, T> from, Transformer<? super T, ?> to) {

        if (state != State.SETUP) throw new IllegalStateException();  // TODO

        addTransformer(from);
        addTransformer(to);

        from.assertCanLinkTo(to);

        Link<T> link = new Link<>(this, from, to);
        from.outputs.add(link);
        to.inputs.add(link);
        return link;
    }

    public <T> Link<T> link(Transformer<?, T> from, Consumer<? super T> consumer) {
        return link(from, new Transformer<>(from.outputClass, Void.class) {
            @Override
            protected void transform(Context context, T input) {
                consumer.accept(input);
            }
        });
    }

    public <T> Link<T> link(Transformer<?, T> from, BiConsumer<Context, ? super T> consumer) {
        return link(from, new Transformer<>(from.outputClass, Void.class) {
            @Override
            protected void transform(Context context, T input) {
                consumer.accept(context, input);
            }
        });
    }

    /**
     * Synchronize the routes from `from` to `to`.
     *
     * This will ensure `to` receives all results from `from` in order
     * and `to` will get a call to `finish(Context)` after it received all
     * emits for a context created by `to`.
     */
    public void sync(String fromName, String toName) {
        sync(getTransformer(fromName), getTransformer(toName));
    }

    /**
     * Synchronize the routes from `from` to `to`.
     *
     * This will ensure `to` receives all results from `from` in order
     * and `to` will get a call to `finish(Context)` after it received all
     * emits for a context created by `to`.
     */
    public void sync(Transformer<?, ?> from, Transformer<?, ?> to) {

        if (state != State.SETUP) {
            throw new IllegalStateException();  // TODO
        }

        List<ArrayDeque<Transformer<?, ?>>> routes = from.getDownstreamRoutes().stream()
                .filter(route -> route.getLast() == to)
                .collect(Collectors.toList());

        if (routes.isEmpty()) {
            throw new IllegalStateException("No route between given transformers");
        }

        from.syncs.add(to);

        Set<Transformer<?, ?>> sanity = routes.stream().map(route ->
                route.stream().reduce((f, t) -> {
                        f.outputs.stream().filter(l -> l.to == t).findFirst().orElseThrow().sync();
                        return t;
                }).orElseThrow()
        ).collect(Collectors.toSet());

        if (!sanity.equals(Set.of(to))) {
            throw new IllegalStateException();  // TODO
        }
    }

    private void seed(Transformer<?, ?> transformer) {
        seed(transformer, null);
    }

    public <T> void seed(Transformer<T, ?> transformer, T seed) {
        addTransformer(transformer);
        transformer.queue.add(new Batch<>(Context.SEED, seed));
    }

    public Configuration toConfiguration() {
        return Configuration.fromGingester(this);
    }

    public void run() {

        if (state != State.SETUP) {
            throw new IllegalStateException();  // TODO
        }

        transformers.forEach(Transformer::setup);
        transformers.stream()
                .filter(Transformer::isEmpty)
                .filter(transformer -> transformer.inputs.isEmpty())
                .forEach(this::seed);

        state = State.RUNNING;

        transformers.stream()
                .filter(transformer -> !transformer.queue.isEmpty())
                .map(this::addWorker)
                .forEach(seeders::add);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::report, 2, 2, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::optimize, 10, 10, TimeUnit.SECONDS);

        while (true) {
            try {
                signals.take().run();
                if (state == State.DONE) break;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);  // TODO
            }
        }

        scheduler.shutdown();
    }

    private void report() {
        signal(() -> {
            // on main thread
        });
    }

    private void optimize() {
        signal(() -> {
            // on main thread
        });
    }

    void signalNewBatch(Transformer<?, ?> transformer) {
        signal(() -> {
            if (transformer.workers.isEmpty()) {
                addWorker(transformer);
            } else {
                for (Worker.Transform worker : transformer.workers) {
                    if (worker.starving) {
                        synchronized (worker.lock) {
                            worker.lock.notify();
                            break;
                        }
                    }
                }
            }
        });
    }

    void signalFull(Transformer<?, ?> transformer) {
        signal(() -> {
            // TODO
        });
    }

    void signalStarving(Worker.Transform worker) {
        signal(() -> {
            if (isWorkerRedundant(worker)) {
                worker.interrupt();
            }
        });
    }

    void signalQuit(Worker.Transform quiter) {
        signal(() -> {
            workers.remove(quiter);
            Transformer<?, ?> transformer = quiter.transformer;
            transformer.workers.remove(quiter);
            if (transformer.isEmpty() && transformer.getUpstream().stream().allMatch(Transformer::isClosed)) {
                transformer.setIsClosing();
                new Worker.Close(this, transformer).start();
            }
        });
    }

    void signalClosed(Transformer<?, ?> transformer) {
        signal(() -> {
            transformer.setIsClosed();
            if (transformers.stream().allMatch(Transformer::isClosed)) {
                state = State.DONE;
            } else {
                maybeClose();
                for (Worker.Transform worker : workers) {
                    if (isWorkerRedundant(worker)) {
                        worker.interrupt();
                    }
                }
            }
        });
    }

    void signalShutdown() {
        signal(() -> seeders.forEach(Worker::interrupt));
    }

    private void signal(Runnable runnable) {
        boolean result = signals.offer(runnable);
        if (!result) throw new IllegalStateException("Too many signals");
    }

    private Worker.Transform addWorker(Transformer<?, ?> transformer) {
        Worker.Transform worker = new Worker.Transform(this, transformer);
        workers.add(worker);
        transformer.workers.add(worker);
        worker.start();
        return worker;
    }

    private void maybeClose() {
        boolean done = true;
        for (Transformer<?, ?> transformer : transformers) {
            if (!transformer.isClosed()) {
                done = false;
                if (transformer.isOpen() && transformer.isEmpty() && transformer.getUpstream().stream().allMatch(Transformer::isClosed)) {
                    transformer.setIsClosing();
                    new Worker.Close(this, transformer).start();
                }
            }
        }
        if (done) state = State.DONE;
    }

    private static boolean isWorkerRedundant(Worker.Transform worker) {

        // a worker is redundant if all its upstream transformers are closed...
        if (worker.transformer.getUpstream().stream().allMatch(Transformer::isClosed)) {

            // ... and it is starving
            synchronized (worker.lock) {
                return worker.starving;
            }
        }

        return false;
    }
}

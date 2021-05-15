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

    private final Map<String, Transformer<?, ?>> transformerMap = new HashMap<>();
    private final Set<Transformer<?, ?>> transformers = new LinkedHashSet<>();
    private final Set<Link<?>> links = new LinkedHashSet<>();
    private final Set<Worker> seeders = new HashSet<>();
    private final Set<Worker> workers = new HashSet<>();
    private final BlockingQueue<Runnable> signals = new LinkedBlockingQueue<>();

    State state = State.SETUP;

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
        transformers.add(transformer);
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
                .filter(t -> Provider.name(t).filter(name::equals).isPresent())
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

    @SuppressWarnings("unchecked")  // checked at runtime
    public <T> Link<T> link(String fromName, String toName) {
        Transformer<?, T> from = (Transformer<?, T>) getTransformer(fromName);
        Transformer<? super T, ?> to = (Transformer<? super T, ?>) getTransformer(toName);
        if (!to.inputClass.isAssignableFrom(from.outputClass)) {
            throw new IllegalArgumentException("Incompatible transformers");
        }
        return link(from, to);
    }

    public <T> Link<T> link(Transformer<?, T> from, Transformer<? super T, ?> to) {

        if (state != State.SETUP) {
            throw new IllegalStateException();  // TODO
        }

        transformers.add(from);
        transformers.add(to);

        Link<T> link = new Link<>(this, from, to);
        from.outputs.add(link);
        to.inputs.add(link);
        links.add(link);
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

        List<Deque<Link<?>>> routes = from.getDownstreamRoutes().stream()
                .filter(route -> route.getLast().to == to)
                .collect(Collectors.toList());

        if (routes.isEmpty()) {
            throw new IllegalStateException("No route between given transformers");
        }

        from.syncs.add(to);
        routes.stream()
                .flatMap(Collection::stream)
                .forEach(Link::sync);
    }

    private void seed(Transformer<?, ?> transformer) {
        seed(transformer, null);
    }

    public <T> void seed(Transformer<T, ?> transformer, T seed) {
        transformers.add(transformer);
        Link<T> link = new Link<>(this, transformer);
        links.add(link);
        transformer.inputs.add(link);
        link.add(new Batch<>(Context.SEED, seed));
    }

    public Configuration toConfiguration() {
        return Configuration.fromGingester(this);
    }

    public void run() {

        if (state != State.SETUP) {
            throw new IllegalStateException();  // TODO
        }

        transformers.stream().filter(t -> t.inputs.isEmpty()).forEach(this::seed);
        links.forEach(Link::setup);
        transformers.forEach(Transformer::setup);

        state = State.RUNNING;

        links.stream().filter(l -> !l.isEmpty())
                .map(this::addWorker)
                .forEach(seeders::add);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::report, 2, 2, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::optimize, 10, 10, TimeUnit.SECONDS);

        while (true) {
            try {
                signals.take().run();
                if (state != State.RUNNING) break;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);  // TODO
            }
        }

        scheduler.shutdown();
    }

    private void report() {

    }

    private void optimize() {

    }

    void signalNewBatch(Link<?> link) {
        signal(() -> {
            if (link.workers.isEmpty()) {
                addWorker(link);
            } else {
                for (Worker worker : link.workers) {
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

    void signalGorged(Link<?> link) {
        signal(() -> {
            // TODO
        });
    }

    void signalStarving(Worker worker) {
        signal(() -> {
            if (isWorkerRedundant(worker)) {
                worker.interrupt();
            }
        });
    }

    void signalQuit(Worker quiter) {
        signal(() -> {
            workers.remove(quiter);
            quiter.link.workers.remove(quiter);
            if (workers.isEmpty()) {
                state = State.DONE;
            } else {
                for (Worker worker : workers) {
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

    private Worker addWorker(Link<?> link) {
        Worker worker = new Worker(this, link);
        workers.add(worker);
        link.workers.add(worker);
        worker.start();
        return worker;
    }

    private static boolean isWorkerRedundant(Worker worker) {

        // a worker is redundant if all its upstream links are empty...
        for (Link<?> link : worker.link.upstream) {
            if (!link.isEmpty()) return false;
        }

        // ... and it is starving
        synchronized (worker.lock) {
            return worker.starving;
        }
    }
}

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

    private final Set<Transformer<?, ?>> transformers;
    final boolean report;

    private final Set<Worker.Transform> seeders = new HashSet<>();
    private final Set<Worker.Transform> workers = new HashSet<>();
    private final BlockingQueue<Runnable> signals = new LinkedBlockingQueue<>();
    final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    State state = State.SETUP;
    private int unopened;
    private int unclosed;

    Gingester(Builder builder) {
        transformers = builder.transformers;
        report = builder.report;
    }

    Set<Transformer<?, ?>> getTransformers() {
        return transformers;
    }

    public Configuration toConfiguration() {
        return Configuration.fromGingester(this);
    }

    public void run() {

        if (state != State.SETUP) {
            throw new IllegalStateException();  // TODO
        }

        transformers.forEach(transformer -> transformer.setup(this));

        // enable statistics on the transformers that have no outputs
        transformers.stream()
                .filter(transformer -> transformer.outgoing.isEmpty())
                .forEach(Transformer::enableStatistics);

        unopened = unclosed = transformers.size();
        transformers.forEach(this::open);
        state = State.RUNNING;

        if (report) scheduler.scheduleAtFixedRate(this::report, 2, 2, TimeUnit.SECONDS);

        while (true) {
            try {
                signals.take().run();
                if (state == State.DONE) break;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);  // TODO
            }
        }

        scheduler.shutdown();

        if (report) report();

        for (Runnable signal : signals) {
            signal.run();
        }
    }

    private void report() {
        signal(() -> {
            for (Transformer<?, ?> transformer : transformers) {
                transformer.getStatistics().ifPresent(statistics -> {
                    statistics.sample();
                    System.err.println(transformer.name + ": " + statistics);
                });
            }
        });
    }

    void signalOpen() {
        signal(() -> {
            if (--unopened == 0) {
                transformers.stream()
                        .filter(transformer -> !transformer.queue.isEmpty())
                        .map(this::addWorker)
                        .forEach(seeders::add);
            }
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
                new Worker.Jobs(this, transformer, transformer::close, () -> signalClosed(transformer)).start();
            }
        });
    }

    void signalClosed(Transformer<?, ?> transformer) {
        signal(() -> {
            transformer.setIsClosed();
            if (--unclosed == 0) {
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

    private void open(Transformer<?, ?> transformer) {
        new Worker.Jobs(this, transformer, transformer::open, this::signalOpen).start();
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
                    new Worker.Jobs(this, transformer, transformer::close, () -> signalClosed(transformer)).start();
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



    // builder

    public static class Builder {

        private final Set<Transformer<?, ?>> transformers = new LinkedHashSet<>();
        private boolean report;
        private boolean built;

        public Builder report(boolean report) {
            this.report = report;
            return this;
        }

        /**
         * Add the given transformer to the to-be-built Gingester.
         *
         * It is not necessary to call this if the transformer was/is also an argument to
         * any of the other methods in this builder.
         */
        void add(Transformer<?, ?> transformer) {
            transformers.add(transformer);
        }

        public Transformer<?, ?> getTransformer(String name) {
            return transformers.stream()
                    .filter(transformer -> transformer.getName().orElseGet(() -> Provider.name(transformer)).equals(name))
                    .reduce((a, b) -> { throw new IllegalStateException("Multiple matches for " + name); })
                    .orElseThrow(() -> new NoSuchElementException("No transformer named " + name));
        }

        @SuppressWarnings("unchecked")  // checked at runtime
        public <T extends Transformer<?, ?>> T getTransformer(String name, Class<T> transformerClass) {
            T transformer = (T) getTransformer(name);
            if (!transformerClass.isInstance(transformer)) throw new ClassCastException();  // TODO
            return transformer;
        }

        /**
         * Allow the given transformer to be referenced by the given name.
         *
         * The name must not have been given to any other transformer.
         * A transformer may only be given 1 name.
         */
        public void name(String name, Transformer<?, ?> transformer) {
            if (transformer.name != null) throw new IllegalArgumentException("Transformer was already named");
            if (transformers.stream().map(Transformer::getName).flatMap(Optional::stream).anyMatch(name::equals)) throw new IllegalArgumentException("Transformer name not unique: " + name);
            transformer.name = name;
            add(transformer);
        }

        public <T> Link<T> link(String fromName, String toName) {
            return linkUnchecked(getTransformer(fromName), getTransformer(toName));
        }

        @SuppressWarnings("unchecked")  // checked at runtime in link()
        <T> Link<T> linkUnchecked(Transformer<?, ?> from, Transformer<?, ?> to) {
            return link((Transformer<?, T>) from, (Transformer<? super T, ?>)to);
        }

        public <T> Link<T> link(Transformer<?, T> from, Transformer<? super T, ?> to) {

            add(from);
            add(to);

            from.assertCanLinkTo(to);

            Link<T> link = new Link<>(from, to);
            from.outgoing.add(link);
            to.incoming.add(link);
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

            List<ArrayDeque<Transformer<?, ?>>> routes = from.getDownstreamRoutes().stream()
                    .filter(route -> route.getLast() == to)
                    .collect(Collectors.toList());

            if (routes.isEmpty()) {
                throw new IllegalStateException("No route between given transformers");
            }

            from.syncs.add(to);

            Set<Transformer<?, ?>> sanity = routes.stream().map(route ->
                    route.stream().reduce((f, t) -> {
                        f.outgoing.stream().filter(l -> l.to == t).findFirst().orElseThrow().requireSync();
                        return t;
                    }).orElseThrow()
            ).collect(Collectors.toSet());

            if (!sanity.equals(Set.of(to))) {
                throw new IllegalStateException();  // TODO
            }
        }

        public <T> void seed(Transformer<T, ?> transformer, T seed) {
            add(transformer);
            transformer.queue.add(new Batch<>(Context.SEED, seed));
        }

        public final Gingester build() {

            if (built) throw new IllegalStateException("Already built");
            built = true;

            // parameter based links
            for (Transformer<?, ?> transformer : transformers) {
                if (transformer.outgoing.isEmpty()) {
                    for (String to : transformer.getLinks()) {
                        linkUnchecked(transformer, getTransformer(to)).markImplied();
                    }
                }
            }

            // seed all transformers that have no inputs and were not already seeded
            for (Transformer<?, ?> transformer : transformers) {
                if (transformer.isEmpty() && transformer.incoming.isEmpty()) {
                    seed(transformer, null);
                }
            }

            Map<String, Integer> counters = new HashMap<>();

            // update every transformer name with a counter suffix
            for (Transformer<?, ?> transformer : transformers) {
                String name = transformer.getName().orElse(Provider.name(transformer));
                Integer counter = counters.get(name);
                counter = counter == null ? 1 : counter + 1;
                counters.put(name, counter);
                transformer.name = name + "-" + counter;
            }

            // remove the counter suffix from transformers with unique names
            for (Transformer<?, ?> transformer : transformers) {
                String name = transformer.getName().orElseThrow();
                if (name.endsWith("-1")) {
                    String nameWithoutSuffix = name.substring(0, name.length() - 2);
                    if (counters.get(nameWithoutSuffix) == 1) {
                        transformer.name = nameWithoutSuffix;
                    }
                }
            }

            return new Gingester(this);
        }
    }
}

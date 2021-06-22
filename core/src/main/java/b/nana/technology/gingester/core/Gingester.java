package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.link.Link;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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

    Gingester(b.nana.technology.gingester.core.Builder builder) {
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
                        .forEach(transformer -> {
                            for (int i = 0; i < transformer.getMaxWorkers(); i++) {
                                seeders.add(addWorker(transformer));
                            }
                        });
            }
        });
    }

    void signalNewBatch(Transformer<?, ?> transformer) {
        signal(() -> {
            if (transformer.workers.isEmpty()) {
                for (int i = 0; i < transformer.getMaxWorkers(); i++) {
                    addWorker(transformer);
                }
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

    public static Builder newBuilder() {
        return new b.nana.technology.gingester.core.Builder();
    }

    public interface Builder {

        Builder report(boolean report);

        /**
         * Add the given transformer to the to-be-built Gingester.
         *
         * It is not necessary to call this if the transformer was/is also an argument to
         * any of the other methods in this builder.
         */
        void add(Transformer<?, ?> transformer);

        /**
         * Allow the given transformer to be referenced by the given name.
         *
         * The name must not have been given to any other transformer.
         * A transformer may only be given 1 name.
         */
        void name(String name, Transformer<?, ?> transformer);

        Transformer<?, ?> getTransformer(String name);
        <T extends Transformer<?, ?>> T getTransformer(String name, Class<T> transformerClass);
        <T> void seed(Transformer<T, ?> transformer, T seed);
        <T> void seed(Transformer<T, ?> transformer, Context.Builder context, T seed);
        Link link(String fromName, String toName);
        <T> Link link(Transformer<?, T> from, Transformer<? super T, ?> to);
        <T> Link link(Transformer<?, T> from, Consumer<? super T> consumer);
        <T> Link link(Transformer<?, T> from, BiConsumer<Context, ? super T> consumer);
        Link except(String fromName, String toName);
        Link except(Transformer<?, ?> from, Transformer<Throwable, ?> to);

        /**
         * Synchronize the routes from `from` to `to`.
         *
         * This will ensure `to` receives all results from `from` in order
         * and `to` will get a call to `finish(Context)` after it received all
         * emits for a context created by `to`.
         */
        void sync(String fromName, String toName);

        /**
         * Synchronize the routes from `from` to `to`.
         *
         * This will ensure `to` receives all results from `from` in order
         * and `to` will get a call to `finish(Context)` after it received all
         * emits for a context created by `to`.
         */
        void sync(Transformer<?, ?> from, Transformer<?, ?> to);

        Gingester build();
    }
}

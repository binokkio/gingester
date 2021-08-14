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

    private final Map<Transformer<?, ?>, Worker> leaders = new HashMap<>();
    private final Set<TransformJob> seeders = new HashSet<>();
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
                .filter(transformer -> transformer.report || transformer.outgoing.isEmpty())
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
                    System.err.println(transformer.id + ": " + statistics);
                });
            }
        });
    }

    void signalOpen() {
        signal(() -> {
            if (--unopened == 0) {
                transformers.stream()
                        .filter(transformer -> !transformer.queue.isEmpty())
                        .map(this::startTransforming)
                        .forEach(seeders::addAll);
            }
        });
    }

    void signalNewBatch(Transformer<?, ?> transformer) {
        signal(() -> {
            if (transformer.transformJobs.isEmpty()) {
                startTransforming(transformer);
            } else {
                for (TransformJob worker : transformer.transformJobs) {
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

    void signalStarving(TransformJob transformJob) {
        signal(() -> interruptIfStarving(transformJob));
    }

    void signalDone(TransformJob transformJob) {
        signal(() -> {
            Transformer<?, ?> transformer = transformJob.transformer;
            transformer.transformJobs.remove(transformJob);
            if (transformer.isEmpty() && transformer.getUpstream().stream().allMatch(Transformer::isClosed)) {
                transformer.setIsClosing();
                leaders.get(transformer).add(transformer::close, () -> signalClosed(transformer));
            }
        });
    }

    void signalClosed(Transformer<?, ?> transformer) {
        signal(() -> {
            transformer.setIsClosed();
            leaders.remove(transformer).interrupt();
            if (--unclosed == 0) {
                state = State.DONE;
            } else {
                maybeClose();
                transformers.stream()
                        .filter(Gingester::isTransformerStarving)
                        .flatMap(t -> t.transformJobs.stream())
                        .forEach(Gingester::interruptIfStarving);
            }
        });
    }

    void signalShutdown() {
        signal(() -> seeders.forEach(transformJob -> transformJob.getWorker().interrupt()));
    }

    private void signal(Runnable runnable) {
        boolean result = signals.offer(runnable);
        if (!result) throw new IllegalStateException("Too many signals");
    }

    private void open(Transformer<?, ?> transformer) {
        Worker leader = new Worker(transformer.id + "_Leader", transformer::open, this::signalOpen);
        leaders.put(transformer, leader);
        leader.start();
    }

    private List<TransformJob> startTransforming(Transformer<?, ?> transformer) {
        List<TransformJob> jobs = new ArrayList<>();
        jobs.add(startTransformJob(leaders.get(transformer), transformer));
        for (int i = 1; i < transformer.maxWorkers; i++) {
            Worker worker = new Worker(transformer.id + "_Worker-" + i);
            worker.start();
            jobs.add(startTransformJob(worker, transformer));
        }
        return jobs;
    }

    private TransformJob startTransformJob(Worker worker, Transformer<?, ?> transformer) {
        TransformJob transformJob = new TransformJob(worker, this, transformer);
        worker.add(transformJob, () -> signalDone(transformJob));
        transformer.transformJobs.add(transformJob);
        return transformJob;
    }

    private void maybeClose() {
        boolean done = true;
        for (Transformer<?, ?> transformer : transformers) {
            if (!transformer.isClosed()) {
                done = false;
                if (transformer.isOpen() && transformer.isEmpty() && transformer.getUpstream().stream().allMatch(Transformer::isClosed)) {
                    transformer.setIsClosing();
                    leaders.get(transformer).add(transformer::close, () -> signalClosed(transformer));
                }
            }
        }
        if (done) state = State.DONE;
    }

    private static boolean isTransformerStarving(Transformer<?, ?> transformer) {
        return transformer.getUpstream().stream().allMatch(Transformer::isClosed);
    }

    private static void interruptIfStarving(TransformJob transformJob) {
        synchronized (transformJob.lock) {
            if (transformJob.starving) {
                transformJob.getWorker().interrupt();
            }
        }
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
         * Allow the given transformer to be referenced by the given id.
         *
         * The id must not have been given to any other transformer.
         * A transformer may only be given 1 id.
         */
        void id(String id, Transformer<?, ?> transformer);

        Transformer<?, ?> getTransformer(String id);
        <T extends Transformer<?, ?>> T getTransformer(String id, Class<T> transformerClass);
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

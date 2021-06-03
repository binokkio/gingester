package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.link.BaseLink;
import b.nana.technology.gingester.core.link.ExceptionLink;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

abstract class Worker extends Thread {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    final Gingester gingester;
    final Transformer<?, ?> transformer;
    final Map<BaseLink<?, ?>, Batch<?>> batches = new HashMap<>();

    Worker(Gingester gingester, Transformer<?, ?> transformer) {
        this.gingester = gingester;
        this.transformer = transformer;
        setName("Gingester-Worker-" + COUNTER.incrementAndGet());
    }

    <T> void accept(Transformer<?, ?> producer, Context context, T value, BaseLink<?, T> link) {

        boolean hasProducerContext = context.transformer == producer;

        if (!hasProducerContext && producer.exceptionHandler != null) {
            context = context.extend(producer).build();
        }

        if (link.isSync()) {

            if (!hasProducerContext && !transformer.syncs.isEmpty()) {
                context = context.extend(producer).build();
            }

            prepare(link.to, context);
            transform(link.to, context, value);
            finish(link.to, context);

            link.to.getStatistics().ifPresent(statistics -> statistics.delt.incrementAndGet());

        } else {

            @SuppressWarnings("unchecked")
            Batch<T> batch = (Batch<T>) batches.get(link);

            if (batch == null) {
                batch = new Batch<>(link.to.batchSize);  // volatile read
                batches.put(link, batch);
            }

            boolean batchFull = batch.addAndIndicateFull(context, value);

            if (batchFull) {
                flush(link, batch);
                batches.remove(link);
            }
        }
    }

    void prepare(Transformer<?, ?> transformer, Context context) {
        for (Transformer<?, ?> sync : transformer.syncs) {
            try {
                sync.prepare(context);
            } catch (Throwable t) {
                handleException(sync, context, t);
            }
        }
    }

    <T> void transform(Transformer<? super T, ?> transformer, Batch<T> batch) {
        for (Batch.Entry<? extends T> value : batch) {
            transform(transformer, value.getContext(), value.getValue());
        }
    }

    <T> void transform(Transformer<? super T, ?> transformer, Context context, T value) {
        try {
            transformer.transform(context, value);
        } catch (Throwable t) {
            handleException(transformer, context, t);
        }
    }

    void finish(Transformer<?, ?> transformer, Context context) {
        for (Transformer<?, ?> sync : transformer.syncs) {
            try {
                sync.finish(context);
            } catch (Throwable t) {
                handleException(sync, context, t);
            }
        }
    }

    void handleException(Transformer<?, ?> transformer, Context exceptionContext, Throwable exception) {

        // keep interrupt flag set
        if (exception instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }

        if (transformer.exceptionHandler != null) {
            accept(transformer, exceptionContext, exception, transformer.exceptionHandler);
        } else {
            for (Context context : exceptionContext) {
                if (context.transformer != null) {
                    ExceptionLink link = context.transformer.exceptionHandler;
                    if (link != null) {
                        accept(transformer, exceptionContext, exception, link);
                        break;
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    <T> void flushAll() {
        for (Map.Entry<BaseLink<?, ?>, Batch<?>> linkBatchEntry : batches.entrySet()) {
            flush((BaseLink<?, T>) linkBatchEntry.getKey(), (Batch<T>) linkBatchEntry.getValue());
        }
    }

    <T> void flush(BaseLink<?, T> link, Batch<? extends T> batch) {
        try {
            link.to.put(batch);
        } catch (InterruptedException e1) {
            try {
                link.to.put(batch);
            } catch (InterruptedException e2) {
                throw new IllegalStateException("Interrupted twice", e2);
            }
            Thread.currentThread().interrupt();
        }
    }

    static class Transform extends Worker {

        final Object lock = new Object();
        volatile boolean starving;
//        long lastBatchReport = System.nanoTime();

        Transform(Gingester gingester, Transformer<?, ?> transformer) {
            super(gingester, transformer);
        }

        @Override
        public void run() {
            run(transformer);
        }

        private <I, O> void run(Transformer<I, O> transformer) {

            try {

                while (true) {

                    Batch<? extends I> batch = transformer.queue.poll();
                    if (batch == null) {
                        synchronized (lock) {
                            starving = true;
                            try {
                                while ((batch = transformer.queue.poll()) == null) {
                                    gingester.signalStarving(this);
                                    lock.wait();
                                }
                            } catch (InterruptedException e) {
                                break;
                            } finally {
                                starving = false;
                            }
                        }
                    }

                    if (transformer.maxBatchSize == 1) {
                        // no batch optimizations possible, not tracking batch duration
                        transform(transformer, batch);
                    } else {

                        long batchStarted = System.nanoTime();
                        transform(transformer, batch);
                        long batchFinished = System.nanoTime();
                        double batchDuration = batchFinished - batchStarted;

                        if ((batchDuration < 2_000_000 && batch.getSize() != transformer.maxBatchSize) ||
                            (batchDuration > 4_000_000 && batch.getSize() != 1)) {

                            double abrupt = 3_000_000 / batchDuration * batch.getSize();
                            double dampened = (abrupt + batch.getSize() * 9) / 10;
                            transformer.batchSize = (int) Math.min(transformer.maxBatchSize, dampened);
                        }

//                        if (lastBatchReport + 1_000_000_000 < batchFinished) {
//                            lastBatchReport = batchFinished;
//                            System.err.printf(
//                                    "%s processed batch of %,d items in %,.3f seconds%n",
//                                    transformer.name,
//                                    batch.getSize(),
//                                    batchDuration / 1_000_000_000
//                            );
//                        }
                    }

                    Transformer.Statistics statistics = transformer.getStatistics().orElse(null);
                    if (statistics != null) statistics.delt.addAndGet(batch.getSize());

                    // TODO flush all batches older than 1 second
                }

                flushAll();

            } catch (Throwable t) {
                t.printStackTrace();  // TODO
                throw t;
            } finally {
                gingester.signalQuit(this);
            }
        }
    }

    static class Jobs extends Worker {

        interface Job {
            void run() throws Exception;
        }

        private final List<Job> jobs;

        Jobs(Gingester gingester, Transformer<?, ?> transformer, Job... jobs) {
            super(gingester, transformer);
            this.jobs = List.of(jobs);
        }

        @Override
        public void run() {
            for (Job job : jobs) {
                try {
                    job.run();
                    flushAll();
                } catch (Exception e) {
                    e.printStackTrace();  // TODO
                }
            }
        }
    }
}

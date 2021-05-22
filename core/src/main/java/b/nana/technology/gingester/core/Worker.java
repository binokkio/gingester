package b.nana.technology.gingester.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

abstract class Worker extends Thread {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    final Gingester gingester;
    final Transformer<?, ?> transformer;
    final Map<Link<?>, Batch<?>> batches = new HashMap<>();

    Worker(Gingester gingester, Transformer<?, ?> transformer) {
        this.gingester = gingester;
        this.transformer = transformer;
        setName("Gingester-Worker-" + COUNTER.incrementAndGet());
    }

    @SuppressWarnings("unchecked")
    <T> void accept(Transformer<?, T> transformer, Context context, T value, int direction) {

        Link<T> link = transformer.outputs.get(direction);

        if (link.sync) {

            // TODO this if misses the case where a transformer is linked to itself, maybe make that illegal?
            if (!transformer.syncs.isEmpty() && context.transformer != transformer) {
                context = context.extend(transformer).build();
            }

            transform(link.to, context, value);
            finish(link.to, context);

            link.to.getStatistics().ifPresent(statistics -> statistics.delt.incrementAndGet());

        } else {

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

    static <T> void transform(Transformer<? super T, ?> transformer, Batch<T> batch) {
        for (Batch.Entry<? extends T> value : batch) {
            transform(transformer, value.context, value.value);
        }
    }

    static <T> void transform(Transformer<? super T, ?> transformer, Context context, T value) {
        try {
            transformer.transform(context, value);
        } catch (InterruptedException e) {
            System.err.println(Provider.name(transformer) + " transform interrupted");
            context.handleException(e);
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            context.handleException(t);
        }
    }

    static void finish(Transformer<?, ?> transformer, Context context) {
        for (Transformer<?, ?> sync : transformer.syncs) {
            try {
                sync.finish(context);
            } catch (InterruptedException e) {
                System.err.println(Provider.name(transformer) + " finish interrupted");
                context.handleException(e);
                Thread.currentThread().interrupt();
            } catch (Throwable t) {
                context.handleException(t);
            }
        }
    }

    @SuppressWarnings("unchecked")
    <T> void flushAll() {
        for (Map.Entry<Link<?>, Batch<?>> linkBatchEntry : batches.entrySet()) {
            flush((Link<T>) linkBatchEntry.getKey(), (Batch<T>) linkBatchEntry.getValue());
        }
    }

    <T> void flush(Link<T> link, Batch<? extends T> batch) {
        try {
            link.to.put(batch);
        } catch (InterruptedException e) {
            for (Batch.Entry<? extends T> entry : batch) {
                entry.context.handleException(e);
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

    static class Close extends Worker {

        Close(Gingester gingester, Transformer<?, ?> transformer) {
            super(gingester, transformer);
        }

        @Override
        public void run() {
            try {
                transformer.close();
            } catch (Exception e) {
                e.printStackTrace();  // TODO
            }
            flushAll();
            gingester.signalClosed(transformer);
        }
    }

    static class Job extends Worker {

        private final Runnable runnable;

        Job(Gingester gingester, Transformer<?, ?> transformer, Runnable runnable) {
            super(gingester, transformer);
            this.runnable = runnable;
        }

        @Override
        public void run() {
            runnable.run();
        }
    }
}

package b.nana.technology.gingester.core;

import java.util.HashMap;
import java.util.Map;

final class Worker extends Thread {

    private static int counter = 0;

    private final Gingester gingester;
    final Transformer<?, ?> transformer;
    private final Runnable runnable;
    final Object lock = new Object();
    private final Map<Link<?>, Batch<?>> batches = new HashMap<>();
    volatile boolean starving;

    Worker(Gingester gingester, Transformer<?, ?> transformer) {
        this(gingester, transformer, null);
    }

    Worker(Gingester gingester, Transformer<?, ?> transformer, Runnable runnable) {
        this.gingester = gingester;
        this.transformer = transformer;
        this.runnable = runnable != null ? runnable : () -> work(transformer);
        setName("Gingester-Worker-" + ++counter);
    }

    @Override
    public void run() {
        runnable.run();
    }

    private <I, O> void work(Transformer<I, O> transformer) {

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

                for (Batch.Entry<? extends I> value : batch) {
                    transform(transformer, value.context, value.value);
                }
            }

            flushAll();

        } catch (Throwable t) {
            t.printStackTrace();  // TODO
            throw t;
        } finally {
            gingester.signalQuit(this);
        }
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

        } else {

            Batch<T> batch = (Batch<T>) batches.get(link);

            if (batch == null) {
                batch = new Batch<>(link.batchSize);  // volatile read
                batches.put(link, batch);
            }

            boolean batchFull = batch.addAndIndicateFull(context, value);

            if (batchFull) {
                flush(link, batch);
                batches.remove(link);
            }
        }
    }

    static <T> void transform(Transformer<? super T, ?> transformer, Context context, T value) {
        try {
            transformer.transform(context, value);
        } catch (InterruptedException e) {
            System.err.println(Provider.name(transformer).orElse("Worker") + " transform interrupted");
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
                System.err.println(Provider.name(transformer).orElse("Worker") + " finish interrupted");
                context.handleException(e);
                Thread.currentThread().interrupt();
            } catch (Throwable t) {
                context.handleException(t);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void flushAll() {
        for (Map.Entry<Link<?>, Batch<?>> linkBatchEntry : batches.entrySet()) {
            flush((Link<T>) linkBatchEntry.getKey(), (Batch<T>) linkBatchEntry.getValue());
        }
    }

    private <T> void flush(Link<T> link, Batch<? extends T> batch) {
        try {
            link.to.put(batch);
        } catch (InterruptedException e) {
            for (Batch.Entry<? extends T> entry : batch) {
                entry.context.handleException(e);
            }
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String toString() {
        return "Worker { transformer: " + Provider.name(transformer) + " }";
    }
}

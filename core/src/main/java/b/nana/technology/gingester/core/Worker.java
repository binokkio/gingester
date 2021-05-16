package b.nana.technology.gingester.core;

import java.util.HashMap;
import java.util.Map;

final class Worker extends Thread {

    private static int counter = 0;

    private final Gingester gingester;
    final Transformer<?, ?> transformer;
    final Object lock = new Object();
    private final Map<Link<?>, Batch<?>> batches = new HashMap<>();
    volatile boolean starving;

    Worker(Gingester gingester, Transformer<?, ?> transformer) {
        this.gingester = gingester;
        this.transformer = transformer;
        setName("Gingester-Worker-" + ++counter);
    }

    @Override
    public void run() {
        try {
            work(transformer);
            flushAll();
        } catch (Throwable t) {
            t.printStackTrace();  // TODO
        } finally {
            gingester.signalQuit(this);
        }
    }

    private <I, O> void work(Transformer<I, O> transformer) {
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
    }

    private <T> void transform(Transformer<? super T, ?> transformer, Context context, T value) {
        try {
            transformer.transform(context, value);
        } catch (InterruptedException e) {
            System.err.println(Provider.name(transformer).orElse("Worker") + " interrupted");
        } catch (Exception e) {
            throw new RuntimeException(e);  // TODO
        }
    }

    @SuppressWarnings("unchecked")
    <T> void accept(Transformer<?, T> transformer, Context context, T value, int direction) {

        Link<T> link = transformer.outputs.get(direction);

        if (!transformer.syncs.isEmpty()) {
            if (context.transformer != transformer) {  // TODO this misses the case where a transformer is linked to itself, maybe make that illegal?
                context = context.extend(transformer).build();
            }
            transformSync(link.to, context, value);
            for (Transformer<?, ?> sync : transformer.syncs) {
                sync.acquirePermit();
                try {
                    sync.finish(context);
                } catch (Exception e) {
                    context.exception(e);
                }
                sync.releasePermit();
            }
        } else if (link.sync) {
            transformSync(link.to, context, value);
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

    private <T> void transformSync(Transformer<? super T, ?> to, Context context, T value) {
        to.acquirePermit();
        transform(to, context, value);
        to.releasePermit();

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
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "Worker { transformer: " + Provider.name(transformer) + " }";
    }
}

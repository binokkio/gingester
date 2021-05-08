package b.nana.technology.gingester.core;

import java.util.HashMap;
import java.util.Map;

final class Worker extends Thread {

    private final Gingester gingester;
    final Link<?> link;
    private final Map<Link<?>, Batch<?>> batches = new HashMap<>();
    volatile boolean starving;

    Worker(Gingester gingester, Link<?> link) {
        this.gingester = gingester;
        this.link = link;
    }

    @Override
    public void run() {

        try {
            if (link.from == null) {
                seed(link);
            } else {
                work(link);
            }
        } catch (Throwable t) {
            t.printStackTrace();  // TODO
        }

        flushAll();
        gingester.signalQuit(this);
    }

    private <T> void seed(Link<T> link) {
        transform(link.to, link.remove());
    }

    private <T> void work(Link<T> link) {
        while (true) {
            Batch<T> batch = link.poll();
            if (batch == null) {
                starving = true;
                gingester.signalStarving(this);
                try {
                    batch = link.take();
                } catch (InterruptedException e) {
                    break;
                }
                starving = false;
            }
            transform(link.to, batch);
        }
    }

    private <T> void transform(Transformer<T, ?> transformer, Batch<T> values) {
        // TODO timing
        for (Batch.Entry<T> value : values) {
            transform(transformer, value.context, value.value);
        }
    }

    private <T> void transform(Transformer<T, ?> transformer, Context context, T value) {
        // TODO timing
        try {
            transformer.transform(context, value);
        } catch (Exception e) {
            throw new RuntimeException(e);  // TODO
        }
    }

    @SuppressWarnings("unchecked")
    <T> void accept(Transformer<?, T> transformer, Context context, T value, int direction) {

        Link<T> link = transformer.outputs.get(direction);

        if (!transformer.syncs.isEmpty()) {
            if (context.transformer != transformer) {  // TODO this misses the case where a transformer is linked to itself
                context = context.extend(transformer).build();
            }
            transform(link.to, context, value);  // TODO could call transform directly on link.to
            for (Transformer<?, ?> sync : transformer.syncs) {
                sync.finish(context);
            }
        } else if (link.sync) {
            transform(link.to, context, value);  // TODO could call transform directly on link.to
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

    @SuppressWarnings("unchecked")
    private <T> void flushAll() {
        for (Map.Entry<Link<?>, Batch<?>> linkBatchEntry : batches.entrySet()) {
            flush((Link<T>) linkBatchEntry.getKey(), (Batch<T>) linkBatchEntry.getValue());
        }
    }

    private <T> void flush(Link<T> link, Batch<T> batch) {
        try {
            link.put(batch);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "Worker { link: " + link + " }";
    }
}

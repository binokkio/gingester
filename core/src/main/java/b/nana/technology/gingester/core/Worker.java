package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.link.BaseLink;
import b.nana.technology.gingester.core.link.ExceptionLink;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

final class Worker extends Thread {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    private static final Job STOP_SENTINEL = () -> {};

    private final BlockingQueue<Job> jobs = new LinkedBlockingQueue<>();
    private final Map<BaseLink<?, ?>, Batch<?>> batches = new HashMap<>();

    Worker(Job... jobs) {
        setName("Gingester-Worker-" + COUNTER.incrementAndGet());
        add(jobs);
    }

    Worker add(Job... jobs) {
        this.jobs.addAll(Arrays.asList(jobs));
        return this;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Job job = jobs.take();
                if (job == STOP_SENTINEL) break;
                job.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
            flushAll();
        }
    }

    public void end() {
        jobs.add(STOP_SENTINEL);
    }

    <T> void accept(Transformer<?, ?> producer, Context context, T value, List<? extends BaseLink<?, T>> links) {
        prepare(producer, context);
        for (BaseLink<?, T> link : links) {
            accept(context, value, link);
        }
        finish(producer, context);
    }

    <T> void accept(Context context, T value, BaseLink<?, T> link) {

        if (link.isSync()) {
            transform(link.to, context, value);
            link.to.getStatistics().ifPresent(statistics -> statistics.delt.incrementAndGet());
        } else {

            @SuppressWarnings("unchecked")
            Batch<T> batch = (Batch<T>) batches.get(link);

            if (batch == null) {
                batch = new Batch<>(link.to.batchSize);  // volatile read
                batches.put(link, batch);
            }

            boolean batchFull = batch.addAndIndicateFull(context, value);

            if (batchFull) {  // TODO also flush if batch is old, maybe have a volatile boolean and a helper thread that sets it true every second and triggers a check of batch.createdAt here
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
        for (Item<? extends T> value : batch) {
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

    private void handleException(Transformer<?, ?> thrower, Context exceptionContext, Throwable exception) {

        // keep interrupt flag set
        if (exception instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }

        if (!thrower.excepts.isEmpty()) {
            for (ExceptionLink except : thrower.excepts) {
                accept(exceptionContext, exception, except);
            }
            return;
        } else {
            for (Context context : exceptionContext) {
                if (context.transformer != null && !context.transformer.excepts.isEmpty()) {
                    for (ExceptionLink except : context.transformer.excepts) {
                        accept(exceptionContext, exception, except);
                    }
                    return;
                }
            }
        }

        exception.printStackTrace();  // TODO
    }

    @SuppressWarnings("unchecked")
    private <T> void flushAll() {
        for (Map.Entry<BaseLink<?, ?>, Batch<?>> linkBatchEntry : batches.entrySet()) {
            flush((BaseLink<?, T>) linkBatchEntry.getKey(), (Batch<T>) linkBatchEntry.getValue());
        }
    }

    private <T> void flush(BaseLink<?, T> link, Batch<? extends T> batch) {
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

    interface Job {
        void run() throws Exception;
    }
}

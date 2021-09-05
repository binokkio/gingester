package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.batch.Batch;
import b.nana.technology.gingester.core.context.Context;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class Worker extends Thread {

    private final Controller<?, ?> controller;
    private final Map<Controller<?, ?>, Batch<?>> batches = new HashMap<>();
    private boolean done;

    Worker(Controller<?, ?> controller, int id) {
        this.controller = controller;
        setName(controller.id + "_" + id);
    }

    @Override
    public void run() {
        while (true) {
            Job job;
            controller.lock.lock();
            try {
                handleFinishingContexts();
                if (done) return;
                while (controller.queue.isEmpty()) {
                    controller.queueNotEmpty.await();
                    handleFinishingContexts();
                    if (done) return;
                }
                job = controller.queue.removeFirst();
                controller.queueNotFull.signal();
                if (job instanceof SyncJob) {
                    perform(job);
                    continue;
                }
            } catch (InterruptedException e) {
                break;
            } finally {
                controller.lock.unlock();
            }
            perform(job);
            flush();
        }
    }

    private void perform(Job job) {
        try {
            job.perform();
        } catch (Exception e) {
            e.printStackTrace();  // TODO pass `e` to `controller.excepts`
        }
    }

    private void handleFinishingContexts() throws InterruptedException {

        Iterator<Map.Entry<Context, FinishTracker>> iterator = controller.finishing.entrySet().iterator();
        while (iterator.hasNext()) {

            Map.Entry<Context, FinishTracker> entry = iterator.next();
            Context context = entry.getKey();
            FinishTracker finishTracker = entry.getValue();

            if (finishTracker.isFullyIndicated()) {
                if (finishTracker.acknowledge(this)) {
                    iterator.remove();

                    if (context.controller.syncs.contains(controller)) {
                        controller.finish(context);
                    }

                    controller.outgoing.values().forEach(controller -> controller.finish(this.controller, context));
                }

                if (context.isSeed()) {
                    done = true;
                }
            }
        }
    }

    public <O> void accept(Context context, O value, Controller<O, ?> target) {

        Batch<O> batch = (Batch<O>) batches.get(target);

        if (batch == null) {
            batch = new Batch<>(target.batchSize);  // volatile read
            batches.put(target, batch);
        }

        boolean batchFull = batch.addAndIndicateFull(context, value);

        if (batchFull) {  // TODO also flush if batch is old, maybe have a volatile boolean and a helper thread that sets it true every second and triggers a check of batch.createdAt here
            flush(target, batch);
            batches.remove(target);
        }
    }

    private <T> void flush() {
        batches.forEach((target, batch) -> flush(
                (Controller<T, ?>) target,
                (Batch<T>) batch
        ));
        batches.clear();
    }

    private <O> void flush(Controller<O, ?> target, Batch<O> batch) {
        target.accept(batch);
    }

    interface Job {
        void perform() throws Exception;
    }

    interface SyncJob extends Job {}
}

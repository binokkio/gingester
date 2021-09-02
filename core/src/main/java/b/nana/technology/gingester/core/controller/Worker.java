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

    Worker(Controller<?, ?> controller) {
        this.controller = controller;
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
            } catch (InterruptedException e) {
                break;
            } finally {
                controller.lock.unlock();
            }
            try {
                job.perform();
                flush();
            } catch (Exception e) {
                throw new RuntimeException(e);  // TODO pass `e` to `controller.excepts`
            }
        }
    }

    private void handleFinishingContexts() {
        Iterator<Map.Entry<Context, FinishTracker>> iterator = controller.finishing.entrySet().iterator();
        while (iterator.hasNext()) {

            Map.Entry<Context, FinishTracker> entry = iterator.next();
            Context context = entry.getKey();
            FinishTracker finishTracker = entry.getValue();

            if (finishTracker.isFullyIndicated()) {
                if (finishTracker.acknowledge(this)) {

                    if (context.controller.syncs.contains(controller)) {
                        controller.finish(context);
                    }

                    System.err.println("Context " + context + " fully acknowledged by " + controller.transformer.getClass().getSimpleName());
                    iterator.remove();
                    controller.outgoing.values().forEach(controller -> controller.finish(controller, entry.getKey()));

                    if (context.isSeed()) {
                        done = true;
                    }
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
    }

    private <O> void flush(Controller<O, ?> target, Batch<O> batch) {
        target.accept(batch);
    }

    interface Job {
        void perform() throws Exception;
    }
}

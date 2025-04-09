package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.item.Batch;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Worker extends Thread {

    private final Controller<?, ?> controller;
    final int id;
    final int mask;
    private final Map<Controller<?, ?>, Batch<?>> batches = new HashMap<>();
    private boolean done;

    Worker(Controller<?, ?> controller, int id) {
        this.controller = controller;
        this.id = id;
        this.mask = 1 << id;
        setName(controller.id + "_" + id);
    }

    @Override
    public void run() {

        if (id == 0) {
            try {
                controller.transformer.open();
            } catch (Exception e) {
                e.printStackTrace();  // TODO set a failure flag and arriveAndAwaitAdvance, then return before the main loop, allowing Gingester to error out completely
                return;
            }
        }

        controller.phaser.arriveAndAwaitAdvance();

        main: while (true) {
            Job job;
            controller.lock.lock();
            try {
                while (true) {
                    while (handleFinishingContexts());
                    if (done) break main;
                    if (controller.queue.isEmpty()) {
                        controller.queueNotEmpty.await();
                    } else {
                        job = controller.queue.removeFirst();
                        controller.queueNotFull.signal();
                        if (job instanceof SyncJob) job.perform();
                        else break;
                    }
                }
            } catch (InterruptedException e) {
                continue;  // the seed finish signal should arrive soon, continue, so we can pass it on
            } finally {
                controller.lock.unlock();
            }
            job.perform();
        }

        flush();
        controller.phaser.arriveAndAwaitAdvance();

        if (id == 0) {
            try {
                controller.transformer.close();
            } catch (Exception e) {
                e.printStackTrace();  // TODO
            }
        }

        controller.phaser.arriveAndAwaitAdvance();
    }

    private boolean handleFinishingContexts() throws InterruptedException {

        boolean flushed = false;

        Map<Context, FinishTracker> copy = new LinkedHashMap<>(controller.finishing);
        for (Map.Entry<Context, FinishTracker> entry : copy.entrySet()) {

            Context context = entry.getKey();
            FinishTracker finishTracker = entry.getValue();

            if (finishTracker.isFullyIndicated() && finishTracker.awaits(this)) {

                if (!flushed) {
                    controller.lock.unlock();
                    flush();
                    controller.lock.lock();
                    flushed = true;
                }

                if (finishTracker.acknowledge(this)) {
                    controller.finishing.remove(context);
                    controller.lock.unlock();
                    controller.finishFinish(context, this);
                    controller.lock.lock();
                }

                done = context.isSeed();
            }
        }

        return flushed;
    }

    public <O> void accept(Context context, O value, Controller<O, ?> target) {

        // noinspection unchecked
        Batch<O> batch = (Batch<O>) batches.get(target);

        if (batch == null) {
            batch = new Batch<>(target.batchSize);  // volatile read
            batches.put(target, batch);
        }

        boolean batchFull = batch.addAndIndicateFull(context, value);

        if (batchFull) {  // TODO also flush if batch is old, maybe have a volatile boolean and a helper thread that sets it true every second and triggers a check of batch.createdAt here
            target.accept(batch);
            batches.remove(target);
        }
    }

    <T> void flush() {
        // noinspection unchecked
        batches.forEach((target, batch) -> ((Controller<T, ?>) target).accept((Batch<T>) batch));
        batches.clear();
    }

    interface Job {
        void perform();
    }

    interface SyncJob extends Job {}
}

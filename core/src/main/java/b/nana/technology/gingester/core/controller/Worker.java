package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.context.Context;

import java.util.Iterator;
import java.util.Map;

final class Worker extends Thread {

    private final Controller<?, ?> controller;
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
                if (done) break;
                while (controller.queue.isEmpty()) {
                    controller.queueNotEmpty.await();
                    handleFinishingContexts();
                    if (done) break;
                }
                if (done) break;
                job = controller.queue.removeFirst();
                controller.queueNotFull.signal();
            } catch (InterruptedException e) {
                break;
            } finally {
                controller.lock.unlock();
            }
            try {
                job.perform();
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

                finishTracker.acknowledged.add(this);

                if (finishTracker.isFullyAcknowledged()) {
                    System.err.println("Context " + context + " fully acknowledged by " + controller.transformer.getClass().getSimpleName());
                    iterator.remove();
                    controller.outgoing.values().forEach(controller -> controller.finish(controller, entry.getKey()));

                    if (entry.getKey() == Context.SEED) {
                        done = true;
                    }
                }
            }
        }
    }

    interface Job {
        void perform() throws Exception;
    }
}

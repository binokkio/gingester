package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.context.Context;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

final class Worker extends Thread {

    private final Controller<?, ?> controller;

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
                while (controller.queue.isEmpty()) {
                    controller.queueNotEmpty.await();
                    handleFinishingContexts();
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
            } catch (Exception e) {
                throw new RuntimeException(e);  // TODO pass `e` to `controller.excepts`
            }
        }
    }

    private void handleFinishingContexts() {
        Iterator<Map.Entry<Context, FinishTracker>> iterator = controller.finishing.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Context, FinishTracker> entry = iterator.next();
            Set<Thread> acknowledged = entry.getValue().acknowledged;
            acknowledged.add(Thread.currentThread());
            if (acknowledged.size() == controller.workers.size()) {
                iterator.remove();
                controller.outgoing.values().forEach(controller -> controller.finish(controller, entry.getKey()));
            }
        }
    }

    interface Job {
        void perform() throws Exception;
    }

    static class FinishTracker {
        final Set<Controller<?, ?>> indicated = new HashSet<>();
        final Set<Thread> acknowledged = new HashSet<>();
    }
}

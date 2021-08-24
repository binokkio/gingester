package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.batch.Batch;
import b.nana.technology.gingester.core.batch.Item;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class Controller<I, O> {

    private final Gingester.ControllerInterface gingester;
    private final Transformer<I, O> transformer;

    private final Parameters parameters;
    private final SimpleSetupControls setupControls;
    private final boolean async;
    private final int queueSize;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition queueNotEmpty = lock.newCondition();
    private final Condition queueNotFull = lock.newCondition();
    private final ArrayDeque<Job> queue = new ArrayDeque<>();
    private final List<Worker> workers = new ArrayList<>();
    private final Set<Controller<?, ?>> incoming = new HashSet<>();
    public final Map<String, Controller<?, ?>> outgoing = new HashMap<>();
    private final Set<Controller<?, ?>> syncs = new HashSet<>();
    private final Set<Controller<?, ?>> excepts = new HashSet<>();
    private final Map<Context, FinishTracker> finishing = new HashMap<>();

    public Controller(String id, Gingester.ControllerInterface gingester, Transformer<I, O> transformer, Parameters parameters) {

        this.gingester = gingester;
        this.transformer = transformer;
        this.parameters = parameters;

        setupControls = new SimpleSetupControls();
        transformer.setup(setupControls);

        async = parameters.async;
        queueSize = parameters.queueSize;
    }

    public void initialize() {

        if (!setupControls.links.isEmpty()) {
            for (String controllerId : setupControls.links) {
                gingester.getController(controllerId).ifPresent(
                        controller -> outgoing.put(controllerId, controller));
            }
        } else {
            for (String controllerId : parameters.links) {
                gingester.getController(controllerId).ifPresent(
                        controller -> outgoing.put(controllerId, controller));
            }
        }

        for (String controllerId : parameters.syncs) {
            gingester.getController(controllerId).ifPresent(syncs::add);
        }

        for (String controllerId : parameters.excepts) {
            gingester.getController(controllerId).ifPresent(excepts::add);
        }
    }

    public void discover() {

    }

    public void start() {

        // queue transformer open runnable
        queue.add(transformer::open);

        // TODO start workers
        for (int i = 0; i < parameters.maxWorkers; i++) {

        }
    }

    public void prepare(Context context) {
        lock.lock();
        try {
            while (queue.size() >= queueSize) queueNotFull.await();
            queue.add(() -> transformer.prepare(context));
            queueNotEmpty.signal();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);  // TODO
        } finally {
            lock.unlock();
        }
    }

    public void accept(Batch<I> batch) {
        lock.lock();
        try {
            while (queue.size() >= queueSize) queueNotFull.await();
            queue.add(() -> transform(batch));
            queueNotEmpty.signal();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);  // TODO
        } finally {
            lock.unlock();
        }
    }

    public void finish(Controller<?, ?> from, Context context) {
        lock.lock();
        finishing.computeIfAbsent(context, x -> new FinishTracker()).indicated.add(from);
        // TODO check if every controller between the `context.controller` and `this` is accounted for
        // TODO e.g. finishing.indicated.size() == TODO.size()
        try {
            while (queue.size() >= queueSize) queueNotFull.await();
            queue.add(() -> {
                lock.lock();
                try {
                    finishing.get(context).acknowledged.add(Thread.currentThread());
                    queueNotEmpty.signalAll();
                } finally {
                    lock.unlock();
                }
            });
            queueNotEmpty.signal();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);  // TODO
        } finally {
            lock.unlock();
        }
    }

    public void stop() {

    }



    //

    public void transform(Batch<I> batch) {
        for (Item<I> item : batch) {
            transform(item.getContext(), item.getValue());
        }
    }

    public void transform(Context context, I input) {
        try {
            transformer.transform(context, input, receiver);
        } catch (Exception e) {
            throw new RuntimeException(e);  // TODO
        }
    }

    private final Receiver<O> receiver = new Receiver<O>() {

        @Override
        public void accept(Context context, O output) {

        }

        @Override
        public void accept(Context context, O output, String target) {
            Controller<?, ?> controller = outgoing.get(target);
            if (controller == null) throw new IllegalStateException("Link not configured!");  // TODO
        }
    };



    //

    private void work() throws InterruptedException {
        Job job;
        lock.lockInterruptibly();
        try {
            handleFinishingContexts();
            while (queue.isEmpty()) {
                queueNotEmpty.await();
                handleFinishingContexts();
            }
            job = queue.removeFirst();
            queueNotFull.signal();
        } finally {
            lock.unlock();
        }
        try {
            job.perform();
        } catch (Exception e) {
            throw new RuntimeException(e);  // TODO check `excepts` first, in `this` and parent context controllers
        }
    }

    private void handleFinishingContexts() {
        Iterator<Map.Entry<Context, FinishTracker>> iterator = finishing.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Context, FinishTracker> entry = iterator.next();
            Set<Thread> acknowledged = entry.getValue().acknowledged;
            acknowledged.add(Thread.currentThread());
            if (acknowledged.size() == workers.size()) {
                iterator.remove();
                outgoing.values().forEach(controller -> controller.finish(this, entry.getKey()));
            }
        }
    }

    interface Job {
        void perform() throws Exception;
    }



    //

    public static class Parameters {
        public String id = null;
        public boolean async = false;
        public List<String> links = Collections.singletonList("__maybe_next__");
        public List<String> syncs = Collections.emptyList();
        public List<String> excepts = Collections.emptyList();
        public int queueSize = 100;
        public int maxWorkers = 1;
        public int maxBatchSize = 65536;
    }



    //

    private static class FinishTracker {
        private final Set<Controller<?, ?>> indicated = new HashSet<>();
        private final Set<Thread> acknowledged = new HashSet<>();
    }
}

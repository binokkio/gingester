package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.batch.Batch;
import b.nana.technology.gingester.core.batch.Item;
import b.nana.technology.gingester.core.configuration.Parameters;
import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformer.TransformerFactory;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class Controller<I, O> {

    private final Parameters parameters;
    private final Gingester.ControllerInterface gingester;
    public final String id;
    public final Transformer<I, O> transformer;

    private final SimpleSetupControls setupControls;
    final boolean async;
    private final int maxQueueSize;
    private final int maxWorkers;
    private final int maxBatchSize;
    volatile int batchSize = 1;

    private final ControllerReceiver<O> receiver = new ControllerReceiver<>(this);
    final ReentrantLock lock = new ReentrantLock();
    final Condition queueNotEmpty = lock.newCondition();
    final Condition queueNotFull = lock.newCondition();
    final ArrayDeque<Worker.Job> queue = new ArrayDeque<>();
    public final List<Worker> workers = new ArrayList<>();

    public final Set<Controller<?, I>> incoming = new HashSet<>();
    public final Map<String, Controller<O, ?>> outgoing = new HashMap<>();
    final Set<Controller<?, ?>> syncs = new HashSet<>();
    final Map<Controller<?, ?>, Set<Controller<?, ?>>> syncedThrough = new HashMap<>();
    private final Set<Controller<?, ?>> excepts = new HashSet<>();
    final Map<Context, FinishTracker> finishing = new HashMap<>();

    public Controller(Parameters parameters, Gingester.ControllerInterface gingester) {
        this(TransformerFactory.instance(parameters), parameters, gingester);
    }

    public Controller(Transformer<I, O> transformer, Parameters parameters, Gingester.ControllerInterface gingester) {

        this.parameters = parameters;
        this.gingester = gingester;
        this.id = gingester.getId();
        this.transformer = transformer;

        setupControls = new SimpleSetupControls();
        transformer.setup(setupControls);

        async = parameters.getAsync();
        maxQueueSize = parameters.getMaxQueueSize();
        maxWorkers = parameters.getMaxWorkers();
        maxBatchSize = parameters.getMaxBatchSize();
    }

    public void initialize() {

        if (!setupControls.links.isEmpty()) {
            for (String controllerId : setupControls.links) {
                gingester.getController(controllerId).ifPresent(
                        controller -> outgoing.put(controllerId, (Controller<O, ?>) controller));
            }
        } else {
            for (String controllerId : parameters.getLinks()) {
                gingester.getController(controllerId).ifPresent(
                        controller -> outgoing.put(controllerId, (Controller<O, ?>) controller));
            }
        }

        for (String controllerId : parameters.getSyncs()) {
            gingester.getController(controllerId).ifPresent(syncs::add);
        }

        for (String controllerId : parameters.getExcepts()) {
            gingester.getController(controllerId).ifPresent(excepts::add);
        }
    }

    public void discover() {

        for (Controller<?, ?> controller : gingester.getControllers()) {
            if (controller.outgoing.containsValue(this)) {
                incoming.add((Controller<?, I>) controller);
            }
        }

        for (Controller<?, ?> controller : gingester.getControllers()) {
            if (controller.syncs.contains(this)) {
                Set<Controller<?, ?>> downstream = controller.getDownstream();
                downstream.retainAll(incoming);
                syncedThrough.put(controller, downstream);
            }
        }
    }

    private Set<Controller<?, ?>> getDownstream() {

        Set<Controller<?, ?>> downstream = new HashSet<>();
        Set<Controller<?, ?>> found = new HashSet<>(outgoing.values());

        while (!found.isEmpty()) {
            downstream.addAll(found);
            Set<Controller<?, ?>> next = new HashSet<>();
            for (Controller<?, ?> controller : found) {
                next.addAll(controller.outgoing.values());
            }
            found = next;
        }

        return downstream;
    }

    public void start() {

        // queue transformer open runnable
        queue.add(transformer::open);

        for (int i = 0; i < maxWorkers; i++) {
            workers.add(new Worker(this));
        }

        workers.forEach(Thread::start);
    }

    public void prepare(Context context) {
        lock.lock();
        try {
            while (queue.size() >= maxQueueSize) queueNotFull.await();
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
            while (queue.size() >= maxQueueSize) queueNotFull.await();
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
        try {
            FinishTracker finishTracker = finishing.computeIfAbsent(context, x -> new FinishTracker(this, context));
            if (finishTracker.indicate(from)) {
                while (queue.size() >= maxQueueSize) queueNotFull.await();
                queue.add(() -> {
                    lock.lock();
                    try {
                        finishTracker.indicate(this);
                        queueNotEmpty.signalAll();
                    } finally {
                        lock.unlock();
                    }
                });
                queueNotEmpty.signal();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);  // TODO
        } finally {
            lock.unlock();
        }
    }



    //

    public void transform(Batch<I> batch) {

        if (maxBatchSize == 1) {
            // no batch optimizations possible, not tracking batch duration
            for (Item<I> item : batch) {
                transform(item.getContext(), item.getValue());
            }
        } else {

            long batchStarted = System.nanoTime();
            for (Item<I> item : batch) {
                transform(item.getContext(), item.getValue());
            }
            long batchFinished = System.nanoTime();
            double batchDuration = batchFinished - batchStarted;

            if ((batchDuration < 2_000_000 && batch.getSize() != maxBatchSize) ||
                    (batchDuration > 4_000_000 && batch.getSize() != 1)) {

                double abrupt = 3_000_000 / batchDuration * batch.getSize();
                double dampened = (abrupt + batch.getSize() * 9) / 10;
                batchSize = (int) Math.min(maxBatchSize, dampened);
            }

//            if (lastBatchReport + 1_000_000_000 < batchFinished) {
//                lastBatchReport = batchFinished;
//                System.err.printf(
//                        "%s processed batch of %,d items in %,.3f seconds%n",
//                        transformer.name,
//                        batch.getSize(),
//                        batchDuration / 1_000_000_000
//                );
//            }
        }
    }

    public void transform(Context context, I input) {
        try {
            transformer.transform(context, input, receiver);
        } catch (Exception e) {
            throw new RuntimeException(e);  // TODO pass `e` to `excepts`
        }
    }

    public void finish(Context context) {
        try {
            transformer.finish(context, receiver);
        } catch (Exception e) {
            throw new RuntimeException(e);  // TODO pass `e` to `excepts`
        }
    }
}

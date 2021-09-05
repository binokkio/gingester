package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.batch.Batch;
import b.nana.technology.gingester.core.batch.Item;
import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.reporting.Counter;
import b.nana.technology.gingester.core.reporting.SimpleCounter;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformer.TransformerFactory;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class Controller<I, O> {

    private final Configuration configuration;
    private final Gingester.ControllerInterface gingester;
    public final String id;
    public final Transformer<I, O> transformer;

    private final SetupControls setupControls;
    final boolean async;
    private final int maxQueueSize;
    private final int maxWorkers;
    private final int maxBatchSize;
    volatile int batchSize = 1;

    public final Set<Controller<?, I>> incoming = new HashSet<>();
    public final Map<String, Controller<O, ?>> outgoing = new HashMap<>();
    final Set<Controller<?, ?>> syncs = new HashSet<>();
    final Map<Controller<?, ?>, Set<Controller<?, ?>>> syncedThrough = new HashMap<>();
    private final Set<Controller<?, ?>> excepts = new HashSet<>();

    final ReentrantLock lock = new ReentrantLock();
    final Condition queueNotEmpty = lock.newCondition();
    final Condition queueNotFull = lock.newCondition();
    final ArrayDeque<Worker.Job> queue = new ArrayDeque<>();
    final Map<Context, FinishTracker> finishing = new HashMap<>();
    public final List<Worker> workers = new ArrayList<>();
    private final ControllerReceiver<O> receiver = new ControllerReceiver<>(this);

    public final boolean report;
    public final Counter delt;
    public final Counter acks;

    public Controller(Configuration configuration, Gingester.ControllerInterface gingester) {

        this.configuration = configuration;
        this.gingester = gingester;
        this.id = gingester.getId();
        this.transformer = (Transformer<I, O>) configuration.getInstance().orElseGet(() -> TransformerFactory.instance(configuration));

        setupControls = new SetupControls();
        transformer.setup(setupControls);

        async = configuration.getAsync();
        maxQueueSize = configuration.getMaxQueueSize();
        maxWorkers = configuration.getMaxWorkers();
        maxBatchSize = configuration.getMaxBatchSize();
        report = configuration.report() != null ? configuration.report() : (setupControls.links.isEmpty() && configuration.getLinks().equals(Collections.singletonList("__maybe_next__")) && !gingester.hasNext());
        acks = setupControls.acksCounter;
        delt = new SimpleCounter(acks != null || report);
    }

    public void initialize() {

        if (setupControls.requireDownstreamSync) {
            String notSync = filterAndName(outgoing.values(), c -> c.async);
            if (!notSync.isEmpty()) {
                throw new IllegalStateException(String.format(
                        "%s requires downstream sync but links to async %s",
                        id, notSync
                ));
            }
        }

        if (setupControls.requireDownstreamAsync) {
            String notAsync = filterAndName(outgoing.values(), c -> !c.async);
            if (!notAsync.isEmpty()) {
                throw new IllegalStateException(String.format(
                        "%s requires downstream async but links to sync %s",
                        id, notAsync
                ));
            }
        }

        if (!setupControls.links.isEmpty()) {
            for (String controllerId : setupControls.links) {
                gingester.getController(controllerId).ifPresent(
                        controller -> outgoing.put(controllerId, (Controller<O, ?>) controller));
            }
        } else {
            for (String controllerId : configuration.getLinks()) {
                gingester.getController(controllerId).ifPresent(
                        controller -> outgoing.put(controllerId, (Controller<O, ?>) controller));
            }
        }

        if (!setupControls.syncs.isEmpty()) {
            for (String controllerId : setupControls.syncs) {
                gingester.getController(controllerId).ifPresent(c -> c.syncs.add(this));
            }
        } else {
            for (String controllerId : configuration.getSyncs()) {
                gingester.getController(controllerId).ifPresent(c -> c.syncs.add(this));
            }
        }

        for (String controllerId : configuration.getExcepts()) {
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
            if (!controller.syncs.isEmpty()) {
                Set<Controller<?, ?>> downstream = controller.getDownstream();
                if (downstream.contains(this)) {
                    downstream.retainAll(incoming);
                    syncedThrough.put(controller, downstream);
                }
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
        // TODO queue.add(start remaining workers) and start only 1 worker, to prevent transform being called before open returns (still some problems with prepare?)

        for (int i = 0; i < maxWorkers; i++) {
            workers.add(new Worker(this, i));
        }

        workers.forEach(Thread::start);
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

    public void prepare(Context context) {
        try {
            transformer.prepare(context, receiver);
        } catch (RuntimeException e) {
            throw e;  // TODO pass `e` to `excepts`
        } catch (Exception e) {
            throw new RuntimeException(e);  // TODO pass `e` to `excepts`
        }
    }

    public void transform(Batch<I> batch) {

        if (maxBatchSize == 1) {
            // no batch optimizations possible, not tracking batch duration
            for (Item<I> item : batch) {
                _transform(item.getContext(), item.getValue());
            }
        } else {

            long batchStarted = System.nanoTime();
            for (Item<I> item : batch) {
                _transform(item.getContext(), item.getValue());
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

        if (report) delt.count(batch.getSize());
    }

    public void transform(Context context, I input) {
        _transform(context, input);
        if (report) delt.count();
    }

    private void _transform(Context context, I input) {
        try {
            transformer.transform(context, input, receiver);
        } catch (RuntimeException e) {
            throw e;  // TODO pass `e` to `excepts`
        } catch (Exception e) {
            throw new RuntimeException(e);  // TODO pass `e` to `excepts`
        }
    }

    public void finish(Context context) {
        try {
            transformer.finish(context, receiver);
        } catch (RuntimeException e) {
            throw e;  // TODO pass `e` to `excepts`
        } catch (Exception e) {
            throw new RuntimeException(e);  // TODO pass `e` to `excepts`
        }
    }



    private String filterAndName(Collection<Controller<O, ?>> controllers, Predicate<Controller<O, ?>> filter) {
        return controllers.stream()
                .filter(filter)
                .map(c -> c.id)
                .sorted()
                .collect(Collectors.joining(", "));
    }
}

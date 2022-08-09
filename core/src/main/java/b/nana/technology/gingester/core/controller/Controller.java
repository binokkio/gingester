package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.ControllerInterface;
import b.nana.technology.gingester.core.batch.Batch;
import b.nana.technology.gingester.core.batch.Item;
import b.nana.technology.gingester.core.configuration.ControllerConfiguration;
import b.nana.technology.gingester.core.reporting.Counter;
import b.nana.technology.gingester.core.reporting.SimpleCounter;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.*;
import java.util.concurrent.Phaser;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public final class Controller<I, O> {

    private final ControllerConfiguration<I, O> configuration;
    final ControllerInterface gingester;
    public final String id;
    public final Transformer<I, O> transformer;
    final Phaser phaser;

    final boolean async;
    private final int maxWorkers;
    final int maxQueueSize;
    private final int maxBatchSize;
    volatile int batchSize = 1;

    public final Map<String, Controller<O, ?>> links = new LinkedHashMap<>();
    public final Map<String, Controller<Exception, ?>> excepts = new LinkedHashMap<>();
    final List<Controller<?, ?>> syncs = new ArrayList<>();
    final Map<Controller<?, ?>, Set<Controller<?, ?>>> syncedThrough = new HashMap<>();
    final Set<Controller<?, ?>> indicatesCoarse = new HashSet<>();
    final Map<Controller<?, ?>, Set<Controller<?, ?>>> indicates = new HashMap<>();
    public final Set<Controller<?, ?>> incoming = new HashSet<>();
    private final Set<Controller<?, ?>> downstream = new HashSet<>();
    public final boolean isExceptionHandler;

    final ReentrantLock lock = new ReentrantLock();
    final Condition queueNotEmpty = lock.newCondition();
    final Condition queueNotFull = lock.newCondition();
    final ArrayDeque<Worker.Job> queue = new ArrayDeque<>();
    final Map<Context, FinishTracker> finishing = new LinkedHashMap<>();
    public final List<Worker> workers = new ArrayList<>();
    final ControllerReceiver<I, O> receiver;

    public final boolean report;
    public final Counter dealt;
    public final Counter acks;

    public Controller(ControllerConfiguration<I, O> configuration, ControllerInterface gingester) {

        this.configuration = configuration;
        this.gingester = gingester;

        id = configuration.getId();
        transformer = configuration.getTransformer();
        receiver = new ControllerReceiver<>(this, gingester.isDebugModeEnabled());
        async = configuration.getMaxWorkers().orElse(0) > 0;
        maxWorkers = configuration.getMaxWorkers().orElse(1);
        maxQueueSize = configuration.getMaxQueueSize().orElse(100);
        maxBatchSize = configuration.getMaxBatchSize().orElse(65536);
        report = configuration.getReport();
        acks = configuration.getAcksCounter();
        dealt = new SimpleCounter(acks != null || report);
        isExceptionHandler = gingester.isExceptionHandler();

        phaser = gingester.getPhaser();
        phaser.bulkRegister(maxWorkers);
    }

    @SuppressWarnings("unchecked")
    public void initialize() {
        configuration.getLinks().forEach((linkName, id) -> links.put(linkName, (Controller<O, ?>) gingester.getController(id).orElseThrow()));
        configuration.getExcepts().forEach(id -> excepts.put(id, (Controller<Exception, ?>) gingester.getController(id).orElseThrow()));
        configuration.getSyncs().forEach(syncId -> gingester.getController(syncId).orElseThrow().syncs.add(this));
    }

    public void discoverIncoming() {
        for (Controller<?, ?> controller : gingester.getControllers()) {
            if (controller.links.containsValue(this)) {
                incoming.add(controller);
                controller.indicatesCoarse.add(this);
            }
            if (controller.excepts.containsValue(this)) {
                Set<Controller<?, ?>> elbbub = elbbub(controller);
                incoming.addAll(elbbub);
                elbbub.forEach(c -> c.indicatesCoarse.add(this));
            }
        }
    }

    public void discoverDownstream() {

        Set<Controller<?, ?>> found = new HashSet<>();
        found.addAll(links.values());
        found.addAll(bubble());

        while (!found.isEmpty()) {
            downstream.addAll(found);
            Set<Controller<?, ?>> next = new HashSet<>();
            for (Controller<?, ?> controller : found) {
                next.addAll(controller.links.values());
                next.addAll(controller.excepts.values());
            }
            found = next;
        }
    }

    public void discoverSyncs() {

        syncs.sort((a, b) -> {
            if (a.downstream.contains(b)) {
                return 1;
            } else if (b.downstream.contains(a)) {
                return -1;
            } else {
                return 0;
            }
        });

        if (incoming.isEmpty()) {  // special handling of the seed controller
            syncedThrough.put(this, Collections.singleton(this));
        } else {
            for (Controller<?, ?> controller : gingester.getControllers()) {
                if (!controller.syncs.isEmpty() || controller.incoming.isEmpty()) {
                    if (controller.downstream.contains(this)) {
                        Set<Controller<?, ?>> downstreamCopy = new HashSet<>(controller.downstream);
                        downstreamCopy.add(controller);
                        downstreamCopy.retainAll(incoming);
                        syncedThrough.put(controller, downstreamCopy);
                    }
                }
            }
        }

        // refine `indicatesCoarse`
        for (Controller<?, ?> controller : gingester.getControllers()) {
            if (!controller.syncs.isEmpty() && (controller == this || controller.downstream.contains(this))) {
                Set<Controller<?, ?>> targets = indicatesCoarse.stream()
                        .filter(c -> controller.syncs.contains(c) || c.downstream.stream().anyMatch(controller.syncs::contains))
                        .collect(Collectors.toSet());
                if (!targets.isEmpty()) indicates.put(controller, targets);
            }
        }

        // seed finish signal is always propagated to whole `indicatesCoarse`
        indicates.put(gingester.getController("__seed__").orElseThrow(), indicatesCoarse);

        receiver.examineController();
    }

    /**
     * @return the exception handlers for `this` controller
     */
    private Set<Controller<?, ?>> bubble() {
        Set<Controller<?, ?>> result = new HashSet<>();
        bubble(this, result);
        return result;
    }

    private void bubble(Controller<?, ?> pointer, Set<Controller<?, ?>> result) {
        if (!pointer.excepts.isEmpty()) {
            result.addAll(pointer.excepts.values());
        } else if (!pointer.isExceptionHandler) {
            for (Controller<?, ?> controller : pointer.incoming) {
                if (controller.links.containsValue(pointer)) {
                    bubble(controller, result);
                }
            }
        }
    }

    /**
     * @param from the elbbub starting point
     * @return the controllers whose exceptions could bubble to `from`
     */
    private Set<Controller<?, ?>> elbbub(Controller<?, ?> from) {
        Set<Controller<?, ?>> result = new HashSet<>();
        elbbub(from, result);
        return result;
    }

    private void elbbub(Controller<?, ?> pointer, Set<Controller<?, ?>> collector) {
        if (pointer.links.isEmpty()) {
            collector.add(pointer);
        } else {
            for (Controller<?, ?> link : pointer.links.values()) {
                if (!link.excepts.isEmpty() || link.isExceptionHandler) {
                    collector.add(pointer);
                } else {
                    elbbub(link, collector);
                }
            }
        }
    }



    public void open() {
        for (int i = 0; i < maxWorkers; i++) {
            Worker worker = new Worker(this, i);
            workers.add(worker);
            worker.start();
        }
    }

    public void accept(Batch<I> batch) {
        lock.lock();
        try {
            while (queue.size() >= maxQueueSize) queueNotFull.awaitUninterruptibly();
            queue.add(() -> transform(batch));
            queueNotEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public void finish(Controller<?, ?> from, Context context) {
        lock.lock();
        try {
            FinishTracker finishTracker = finishing.computeIfAbsent(context, x -> FinishTracker.newInstance(this, context));
            if (finishTracker.indicate(from)) {
                // not checking if the queue is full, finish signals have their own backpressure system
                queue.add((Worker.SyncJob) () -> {
                    finishTracker.indicate(this);
                    queueNotEmpty.signalAll();
                });
                queueNotEmpty.signal();
            }
        } finally {
            lock.unlock();
        }
    }



    //
    // NOTE: some duplicate try-catch blocks in the following methods to keep the call stack smaller, makes for nicer profiling

    public void prepare(Context context) {
        try {
            transformer.prepare(context, receiver);
        } catch (Exception e) {
            receiver.except("prepare", context, e);
        }
    }

    public void transform(Batch<I> batch) {

        Context peek = batch.peek().getContext();

        if (maxBatchSize == 1 || batch.getSize() != batchSize) {
            try {
                transformer.beforeBatch(peek);
            } catch (Exception e) {
                receiver.except("beforeBatch", peek, e);
            }
            for (Item<I> item : batch) {
                try {
                    transformer.transform(item.getContext(), item.getValue(), receiver);
                } catch (Exception e) {
                    receiver.except("transform", item.getContext(), item.getValue(), e);
                }
            }
            try {
                transformer.afterBatch(peek);
            } catch (Exception e) {
                receiver.except("afterBatch", peek, e);
            }
        } else {

            try {
                transformer.beforeBatch(batch.peek().getContext());
            } catch (Exception e) {
                receiver.except("beforeBatch", peek, e);
            }
            long batchStarted = System.nanoTime();
            for (Item<I> item : batch) {
                try {
                    transformer.transform(item.getContext(), item.getValue(), receiver);
                } catch (Exception e) {
                    receiver.except("transform", item.getContext(), item.getValue(), e);
                }
            }
            long batchFinished = System.nanoTime();
            try {
                transformer.afterBatch(batch.peek().getContext());
            } catch (Exception e) {
                receiver.except("afterBatch", peek, e);
            }
            long batchDuration = batchFinished - batchStarted;

            if ((batchDuration < 2_000_000 && batch.getSize() != maxBatchSize) ||
                (batchDuration > 4_000_000 && batch.getSize() != 1)) {

                double abrupt = 3_000_000d / batchDuration * batch.getSize();
                double dampened = (abrupt + batch.getSize() * 9) / 10;
                batchSize = (int) Math.min(maxBatchSize, dampened);
            }

//            if (lastBatchReport + 1_000_000_000 < batchFinished) {
//                lastBatchReport = batchFinished;
//                System.err.printf(
//                        "%s processed batch of %,d items in %,.3f seconds%n",
//                        id,
//                        batch.getSize(),
//                        batchDuration / 1_000_000_000
//                );
//            }
        }

        if (report) dealt.count(batch.getSize());
    }

    public void transform(Context context, I in) {
        try {
            transformer.beforeBatch(context);
        } catch (Exception e) {
            receiver.except("beforeBatch", context, in, e);
        }
        try {
            transformer.transform(context, in, receiver);
        } catch (Exception e) {
            receiver.except("transform", context, in, e);
        }
        try {
            transformer.afterBatch(context);
        } catch (Exception e) {
            receiver.except("afterBatch", context, in, e);
        }
        if (report) dealt.count();
    }

    public void finish(Context context) {
        try {
            transformer.finish(context, receiver);
        } catch (Exception e) {
            receiver.except("finish", context, e);
        }
    }



    Controller(String controllerId) {
        configuration = null;
        gingester = null;
        id = controllerId;
        transformer = null;
        receiver = null;
        phaser = null;
        async = false;
        maxWorkers = 0;
        maxQueueSize = 0;
        maxBatchSize = 0;
        report = false;
        dealt = null;
        acks = null;
        isExceptionHandler = false;
    }
}

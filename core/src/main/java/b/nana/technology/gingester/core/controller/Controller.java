package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.batch.Batch;
import b.nana.technology.gingester.core.batch.Item;
import b.nana.technology.gingester.core.configuration.ControllerConfiguration;
import b.nana.technology.gingester.core.reporting.Counter;
import b.nana.technology.gingester.core.reporting.SimpleCounter;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class Controller<I, O> {

    private final ControllerConfiguration<I, O> configuration;
    final Gingester.ControllerInterface gingester;
    public final String id;
    public final Transformer<I, O> transformer;

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
    final Map<Context, FinishTracker> finishing = new LinkedHashMap<>();
    public final List<Worker> workers = new ArrayList<>();
    public final List<Worker> done = new ArrayList<>();
    private final ControllerReceiver<O> receiver = new ControllerReceiver<>(this);

    public final boolean report;
    public final Counter delt;
    public final Counter acks;

    public Controller(ControllerConfiguration<I, O> configuration, Gingester.ControllerInterface gingester) {

        this.configuration = configuration;
        this.gingester = gingester;

        id = configuration.getId();
        transformer = configuration.getTransformer();
        async = configuration.getMaxWorkers().orElse(0) > 0;
        maxBatchSize = configuration.getMaxBatchSize().orElse(65536);
        maxQueueSize = configuration.getMaxQueueSize().orElse(100);
        maxWorkers = configuration.getMaxWorkers().orElse(1);
        report = configuration.getReport();
        acks = configuration.getAcksCounter();
        delt = new SimpleCounter(acks != null || report);
    }

    public List<String> getLinks() {
        return configuration.getLinks();
    }

    public List<String> getSyncs() {
        return configuration.getSyncs();
    }

    public List<String> getExcepts() {
        return configuration.getExcepts();
    }

    public void initialize() {
        getLinks().forEach(linkId -> gingester.getController(linkId).ifPresent(controller -> outgoing.put(linkId, (Controller<O, ?>) controller)));
        getSyncs().forEach(syncId -> gingester.getController(syncId).ifPresent(sync -> sync.syncs.add(this)));
        getExcepts().forEach(exceptId -> gingester.getController(exceptId).ifPresent(excepts::add));
    }

    public void discover() {

        for (Controller<?, ?> controller : gingester.getControllers()) {
            if (controller.outgoing.containsValue(this)) {
                incoming.add((Controller<?, I>) controller);
            }
        }

        for (Controller<?, ?> controller : gingester.getControllers()) {
            if (!controller.syncs.isEmpty() || controller.incoming.isEmpty()) {
                Set<Controller<?, ?>> downstream = controller.getDownstream();
                downstream.add(controller);
                if (downstream.contains(this)) {
                    downstream.retainAll(incoming);
                    if (!downstream.isEmpty()) {
                        syncedThrough.put(controller, downstream);
                    }
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

    public void open() {
        for (int i = 0; i < maxWorkers; i++) {
            workers.add(new Worker(this, i));
        }
        queue.add(transformer::open);
        queue.add(gingester::signalOpen);
        workers.get(0).start();
    }

    public void start() {
        for (int i = 1; i < workers.size(); i++) {
            workers.get(i).start();
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
//                while (queue.size() >= maxQueueSize) queueNotFull.await();  // TODO deadlocks PackTest.testPackIndividually(), look into
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
            handleException(e);
        }
    }

    public void transform(Batch<I> batch) {

        if (maxBatchSize == 1) {
            // no batch optimizations possible, not tracking batch duration
            for (Item<I> item : batch) {
                try {
                    transformer.transform(item.getContext(), item.getValue(), receiver);
                } catch (Exception e) {
                    handleException(e);
                }
            }
        } else {

            long batchStarted = System.nanoTime();
            for (Item<I> item : batch) {
                try {
                    transformer.transform(item.getContext(), item.getValue(), receiver);
                } catch (Exception e) {
                    handleException(e);
                }
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
        try {
            transformer.transform(context, input, receiver);
        } catch (Exception e) {
            handleException(e);
        }
        if (report) delt.count();
    }

    public void finish(Context context) {
        try {
            transformer.finish(context, receiver);
        } catch (Exception e) {
            handleException(e);
        }
    }

    private void handleException(Exception e) {
        e.printStackTrace();  // TODO pass `e` to `excepts`
    }



    Controller() {
        configuration = null;
        gingester = null;
        id = "__unknown__";
        transformer = null;
        async = false;
        maxQueueSize = 0;
        maxWorkers = 0;
        maxBatchSize = 0;
        report = false;
        delt = null;
        acks = null;
    }
}

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

    final Map<String, Controller<O, ?>> links;
    final Set<Controller<?, ?>> syncs = new HashSet<>();
    final Map<Controller<?, ?>, Set<Controller<?, ?>>> syncedThrough = new HashMap<>();
    final Map<String, Controller<Exception, ?>> excepts;
    public final Set<Controller<?, I>> incoming = new HashSet<>();
    public final Map<String, Controller<?, ?>> outgoing;

    final ReentrantLock lock = new ReentrantLock();
    final Condition queueNotEmpty = lock.newCondition();
    final Condition queueNotFull = lock.newCondition();
    final ArrayDeque<Worker.Job> queue = new ArrayDeque<>();
    final Map<Context, FinishTracker> finishing = new LinkedHashMap<>();
    public final List<Worker> workers = new ArrayList<>();
    public final List<Worker> done = new ArrayList<>();
    private final ControllerReceiver<I, O> receiver = new ControllerReceiver<>(this);

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

        links = configuration.getLinks().stream().collect(
                LinkedHashMap::new,
                (map, link) -> map.put(link, null),
                Map::putAll
        );

        excepts = configuration.getExcepts().stream().collect(
                LinkedHashMap::new,
                (map, link) -> map.put(link, null),
                Map::putAll
        );

        outgoing = new LinkedHashMap<>();
        outgoing.putAll(links);
        outgoing.putAll(excepts);
    }

    public void initialize() {
        links.replaceAll((id, nullController) -> (Controller<O, ?>) gingester.getController(id).orElseThrow());
        excepts.replaceAll((id, nullController) -> (Controller<Exception, ?>) gingester.getController(id).orElseThrow());
        outgoing.putAll(links);
        outgoing.putAll(excepts);
        configuration.getSyncs().forEach(syncId -> gingester.getController(syncId).orElseThrow().syncs.add(this));
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

        // special handling of the seed controller
        if (incoming.isEmpty()) {
            syncedThrough.put(this, Set.of(this));
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
            receiver.except("prepare", context, e);
        }
    }

    public void transform(Batch<I> batch) {

        if (maxBatchSize == 1) {
            // no batch optimizations possible, not tracking batch duration
            for (Item<I> item : batch) {
                try {
                    transformer.transform(item.getContext(), item.getValue(), receiver);
                } catch (Exception e) {
                    receiver.except("transform", item.getContext(), item.getValue(), e);
                }
            }
        } else {

            long batchStarted = System.nanoTime();
            for (Item<I> item : batch) {
                try {
                    transformer.transform(item.getContext(), item.getValue(), receiver);
                } catch (Exception e) {
                    receiver.except("transform", item.getContext(), item.getValue(), e);
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
//                        id,
//                        batch.getSize(),
//                        batchDuration / 1_000_000_000
//                );
//            }
        }

        if (report) delt.count(batch.getSize());
    }

    public void transform(Context context, I in) {
        try {
            transformer.transform(context, in, receiver);
        } catch (Exception e) {
            receiver.except("transform", context, in, e);
        }
        if (report) delt.count();
    }

    public void finish(Context context) {
        try {
            transformer.finish(context, receiver);
        } catch (Exception e) {
            receiver.except("finish", context, e);
        }
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
        links = Collections.emptyMap();  // TODO sort
        excepts = Collections.emptyMap();  // TODO sort
        outgoing = Collections.emptyMap();  // TODO sort
    }
}

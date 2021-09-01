package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.batch.Batch;
import b.nana.technology.gingester.core.batch.Item;
import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public final class Controller<I, O> {

    private final String id;
    private final Gingester.ControllerInterface gingester;
    final Transformer<I, O> transformer;
    private final Parameters parameters;

    private final SimpleSetupControls setupControls;
    final boolean async;
    private final int queueSize;

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

    public Controller(String id, Gingester.ControllerInterface gingester, Transformer<I, O> transformer, Parameters parameters) {

        this.id = id;
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
                        controller -> outgoing.put(controllerId, (Controller<O, ?>) controller));
            }
        } else {
            for (String controllerId : parameters.links) {
                gingester.getController(controllerId).ifPresent(
                        controller -> outgoing.put(controllerId, (Controller<O, ?>) controller));
            }
        }

        System.err.println(id + " " + outgoing.values().stream().map(oController -> oController.id).collect(Collectors.joining(", ")));

        for (String controllerId : parameters.syncs) {
            gingester.getController(controllerId).ifPresent(syncs::add);
        }

        for (String controllerId : parameters.excepts) {
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

        for (int i = 0; i < parameters.maxWorkers; i++) {
            workers.add(new Worker(this));
        }

        workers.forEach(Thread::start);
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
        try {
            FinishTracker finishTracker = finishing.computeIfAbsent(context, x -> new FinishTracker(this, context));
            if (finishTracker.indicate(from)) {
                while (queue.size() >= queueSize) queueNotFull.await();
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
        for (Item<I> item : batch) {
            transform(item.getContext(), item.getValue());
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
}

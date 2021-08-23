package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class Controller<I, O> {

    private final Gingester.DriverInterface gingester;
    private final Transformer<I, O> transformer;

    private final Parameters parameters;
    private final boolean async;
    private final int queueSize;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition queueNotEmpty = lock.newCondition();
    private final Condition queueNotFull = lock.newCondition();
    private final ArrayDeque<Job> queue = new ArrayDeque<>();
    private final List<Worker> workers = new ArrayList<>();
    private final Map<String, Controller<?, ?>> links = new HashMap<>();
    private final Set<Controller<?, ?>> syncs = new HashSet<>();
    private final Set<Controller<?, ?>> excepts = new HashSet<>();
    private final Map<Context, Set<Worker>> finishing = new HashMap<>();

    public Controller(String id, Gingester.DriverInterface gingester, Transformer<I, O> transformer, Parameters parameters) {

        this.gingester = gingester;
        this.transformer = transformer;
        this.parameters = parameters;

        async = parameters.async;
        queueSize = parameters.queueSize;

        SimpleSetupControls simpleSetupControls = new SimpleSetupControls();
        transformer.setup(simpleSetupControls);
        // TODO process transformer setup controls
    }

    public void initialize() {

        // TODO fill `links`

        // TODO fill `syncs`

        // TODO queue transformer open runnable
        queue.add((Worker worker) -> transformer.open());

        // TODO start workers
        for (int i = 0; i < parameters.maxWorkers; i++) {

        }
    }

    public void prepare(Context context) {

    }

    public void accept() {

    }

    public void finish(Context context) {
        lock.lock();
        // TODO check if every controller between the `context.controller` and `this` is accounted for
        try {
            queue.add((Worker worker) -> {
                lock.lock();
                try {
                    Set<Worker> acknowledged = new HashSet<>();
                    acknowledged.add(worker);
                    finishing.put(context, acknowledged);
                    queueNotEmpty.signalAll();
                } finally {
                    lock.unlock();
                }
            });
            queueNotEmpty.signal();
        } finally {
            lock.unlock();
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
            Controller<?, ?> controller = links.get(target);
            if (controller == null) throw new IllegalStateException("Link not configured!");  // TODO
        }
    };



    //

    private void work(Worker worker) throws InterruptedException {
        Job job;
        lock.lockInterruptibly();
        try {
            handleFinishingContexts(worker);
            while (queue.isEmpty()) {
                queueNotEmpty.await();
                handleFinishingContexts(worker);
            }
            job = queue.removeFirst();
        } finally {
            lock.unlock();
        }
        try {
            job.perform(worker);
        } catch (Exception e) {
            throw new RuntimeException(e);  // TODO check `excepts` first, in `this` and parent context controllers
        }
    }

    private void handleFinishingContexts(Worker worker) {
        Iterator<Map.Entry<Context, Set<Worker>>> iterator = finishing.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Context, Set<Worker>> entry = iterator.next();
            Set<Worker> acknowledged = entry.getValue();
            acknowledged.add(worker);
            if (acknowledged.size() == workers.size()) {
                iterator.remove();
                // TODO queue.addFirst(call transformer finish with `entry.getKey`)
            }
        }
    }

    interface Job {
        void perform(Worker worker) throws Exception;
    }



    //

    public static class Parameters {
        public String id;
        public boolean async;
        public List<String> defaultTargets;
        public int queueSize;
        public int maxWorkers;
        public int maxBatchSize;
    }
}

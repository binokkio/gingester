package b.nana.technology.gingester.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Predicate;

public final class Link<T> {

    private final Gingester gingester;
    final Transformer<?, T> from;
    final Set<Link<?>> upstream = new HashSet<>();
    final Transformer<T, ?> to;
    final Set<Link<?>> downstream = new HashSet<>();
    final Set<Link<?>> syncedDownstream = new HashSet<>();
    final Set<Worker> workers = new HashSet<>();
    private final BlockingQueue<Batch<T>> queue = new ArrayBlockingQueue<>(100);
    boolean sync = false;
    volatile int batchSize = 1;
    volatile boolean gorged;

    Link(Gingester gingester, Transformer<T, ?> to) {
        this(gingester, null, to);
    }

    Link(Gingester gingester, Transformer<?, T> from, Transformer<T, ?> to) {
        this.gingester = gingester;
        this.from = from;
        this.to = to;
    }

    /**
     * Synchronize this link.
     *
     * This will ensure the downstream transform is performed on the same thread
     * immediately after each `emit(..)` by the upstream transformer.
     */
    public void sync() {
        if (gingester.getState() != Gingester.State.LINKING) throw new IllegalStateException();
        sync = true;
    }

    void setup() {
        discover(true, this, l -> true, upstream);
        discover(false, this, l -> true, downstream);
        discover(false, this, l -> l.sync, syncedDownstream);
    }

    boolean add(Batch<T> batch) {
        return queue.add(batch);
    }

    void put(Batch<T> batch) throws InterruptedException {
        boolean accepted = queue.offer(batch);
        if (!accepted) {
            gorged = true;
            gingester.signalGorged(this);
            queue.put(batch);
            gorged = false;
        }
        gingester.signalNewBatch(this);
    }

    Batch<T> poll() {
        return queue.poll();
    }

    Batch<T> remove() {
        return queue.remove();
    }

    Batch<T> take() throws InterruptedException {
        return queue.take();
    }

    boolean isEmpty() {
        return queue.isEmpty() && workers.isEmpty();
    }

    @Override
    public String toString() {
        return "Link { from: " + (from == null ? "<seed>" : from.getClass().getSimpleName()) + ", to: " + to.getClass().getSimpleName() + " }";
    }

    private static void discover(boolean upstream, Link<?> link, Predicate<Link<?>> predicate, Set<Link<?>> target) {
        Transformer<?, ?> transformer = upstream ? link.from : link.to;
        if (transformer != null) {
            List<?> links = upstream ? transformer.inputs : transformer.outputs;
            for (Object o : links) {
                Link<?> l = (Link<?>) o;
                if (predicate.test(l)) {
                    boolean recurse = target.add(l);
                    if (recurse) discover(upstream, (Link<?>) o, predicate, target);
                }
            }
        }
    }
}

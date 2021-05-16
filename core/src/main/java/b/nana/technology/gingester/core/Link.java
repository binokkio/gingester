package b.nana.technology.gingester.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class Link<T> {

    private final Gingester gingester;
    final Transformer<?, T> from;
    final Set<Link<?>> upstream = new HashSet<>();
    final Transformer<? super T, ?> to;
    final Set<Link<?>> downstream = new HashSet<>();
    final Set<Link<?>> syncedDownstream = new HashSet<>();
    boolean sync = false;
    volatile int batchSize = 1;

    Link(Gingester gingester, Transformer<?, T> from, Transformer<? super T, ?> to) {
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
        if (gingester.state != Gingester.State.SETUP) throw new IllegalStateException();
        sync = true;
    }

    void setup() {
        discover(true, this, l -> true, upstream);
        discover(false, this, l -> true, downstream);
        discover(false, this, l -> l.sync, syncedDownstream);
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

package b.nana.technology.gingester.core;

public final class Link<T> {

    private final Gingester gingester;
    final Transformer<?, T> from;
    final Transformer<? super T, ?> to;
    boolean sync = false;
    int maxBatchSize = 10000;
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

    void limitBatchSize(int maxBatchSize) {
        this.maxBatchSize = Math.min(this.maxBatchSize, maxBatchSize);
    }

    @Override
    public String toString() {
        return "Link { from: " + (from == null ? "<seed>" : from.getClass().getSimpleName()) + ", to: " + to.getClass().getSimpleName() + " }";
    }
}

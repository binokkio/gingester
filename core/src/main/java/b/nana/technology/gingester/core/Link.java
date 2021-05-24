package b.nana.technology.gingester.core;

public final class Link<T> {

    final Transformer<?, T> from;
    final Transformer<? super T, ?> to;
    private boolean sync;
    private boolean explicitSync;

    Link(Transformer<?, T> from, Transformer<? super T, ?> to) {
        this.from = from;
        this.to = to;
    }

    boolean isSync() {
        return sync;
    }

    boolean isExplicitSync() {
        return explicitSync;
    }

    /**
     * Synchronize this link.
     *
     * This will ensure the downstream transform is performed on the same thread
     * immediately after each `emit(..)` by the upstream transformer.
     */
    public void sync() {
        sync = true;
        explicitSync = true;
    }

    void syncImplied() {
        sync = true;
    }

    @Override
    public String toString() {
        return
                "Link { from: " +
                from.getName().orElseGet(() -> Provider.name(from)) +
                ", to: " +
                to.getName().orElseGet(() -> Provider.name(to)) +
                " }";
    }
}

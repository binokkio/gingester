package b.nana.technology.gingester.core;

public final class Link<T> {

    final Transformer<?, T> from;
    final Transformer<? super T, ?> to;
    private boolean sync = true;
    private boolean syncModeExplicit;
    private boolean syncModeRequired;

    Link(Transformer<?, T> from, Transformer<? super T, ?> to) {
        this.from = from;
        this.to = to;
    }

    boolean isSync() {
        return sync;
    }

    boolean isSyncModeExplicit() {
        return syncModeExplicit;
    }

    /**
     * Synchronize this link.
     *
     * This will ensure the downstream transform is performed on the same thread
     * immediately after each `emit(..)` by the upstream transformer.
     */
    public void sync() {
        if (syncModeRequired && !sync) throw new IllegalStateException("`sync()` called on link that has async requirement");
        if (syncModeExplicit && !sync) throw new IllegalStateException("`sync()` called on link that has explicit async");
        sync = true;
        syncModeExplicit = true;
    }

    /**
     * Make this link asynchronous.
     *
     * This will ensure the downstream transform is performed on a different thread
     * some time after each `emit(..)` by the upstream transformer.
     */
    public void async() {
        if (syncModeRequired && sync) throw new IllegalStateException("`async()` called on link that has sync requirement");
        if (syncModeExplicit && sync) throw new IllegalStateException("`async()` called on link that has explicit sync");
        sync = false;
        syncModeExplicit = true;
    }

    void requireSync() {
        if (syncModeRequired && !sync) throw new IllegalStateException("`requireSync()` called on link that has async requirement");
        if (syncModeExplicit && !sync) throw new IllegalStateException("`requireSync()` called on link that has explicit async");
        sync = true;
        syncModeRequired = true;
    }

    void requireAsync() {
        if (syncModeRequired && sync) throw new IllegalStateException("`requireAsync()` called on link that has sync requirement");
        if (syncModeExplicit && sync) throw new IllegalStateException("`requireAsync()` called on link that has explicit sync");
        sync = false;
        syncModeRequired = true;
    }

    void preferAsync() {
        if (!syncModeRequired && !syncModeExplicit) {
            sync = false;
        }
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

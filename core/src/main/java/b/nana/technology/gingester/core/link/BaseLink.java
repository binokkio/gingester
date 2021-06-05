package b.nana.technology.gingester.core.link;

import b.nana.technology.gingester.core.Transformer;

public abstract class BaseLink<F, T> implements Link {

    public final Transformer<?, F> from;
    public final Transformer<? super T, ?> to;
    private boolean implied;
    private boolean sync = true;
    private boolean syncModeExplicit;
    private boolean syncModeRequired;

    protected BaseLink(Transformer<?, F> from, Transformer<? super T, ?> to) {
        this.from = from;
        this.to = to;
    }

    public void markImplied() {
        implied = true;
    }

    public boolean isImplied() {
        return implied;
    }

    public boolean isSync() {
        return sync;
    }

    public boolean isSyncModeExplicit() {
        return syncModeExplicit;
    }

    @Override
    public void sync() {
        if (syncModeRequired && !sync) throw new IllegalStateException("`sync()` called on link that has async requirement");
        if (syncModeExplicit && !sync) throw new IllegalStateException("`sync()` called on link that has explicit async");
        sync = true;
        syncModeExplicit = true;
    }

    @Override
    public void async() {
        if (syncModeRequired && sync) throw new IllegalStateException("`async()` called on link that has sync requirement");
        if (syncModeExplicit && sync) throw new IllegalStateException("`async()` called on link that has explicit sync");
        sync = false;
        syncModeExplicit = true;
    }

    public void requireSync() {
        if (syncModeRequired && !sync) throw new IllegalStateException("`requireSync()` called on link that has async requirement");
        if (syncModeExplicit && !sync) throw new IllegalStateException("`requireSync()` called on link that has explicit async");
        sync = true;
        syncModeRequired = true;
    }

    public void requireAsync() {
        if (syncModeRequired && sync) throw new IllegalStateException("`requireAsync()` called on link that has sync requirement");
        if (syncModeExplicit && sync) throw new IllegalStateException("`requireAsync()` called on link that has explicit sync");
        sync = false;
        syncModeRequired = true;
    }

    public void preferAsync() {
        if (!syncModeRequired && !syncModeExplicit) {
            sync = false;
        }
    }
}

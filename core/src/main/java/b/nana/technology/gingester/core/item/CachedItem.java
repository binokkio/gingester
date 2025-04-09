package b.nana.technology.gingester.core.item;

import b.nana.technology.gingester.core.common.InputStreamReplicator;

import java.util.Map;

public final class CachedItem<T> {

    private final Map<String, ?> stash;
    private final T value;
    private final String targetId;

    public CachedItem(T value) {
        this.stash = null;
        this.value = value;
        this.targetId = null;
    }

    public CachedItem(Map<String, ?> stash, T value) {
        this.stash = stash;
        this.value = value;
        this.targetId = null;
    }

    public CachedItem(T value, String targetId) {
        this.stash = null;
        this.value = value;
        this.targetId = targetId;
    }

    public CachedItem(Map<String, ?> stash, T value, String targetId) {
        this.stash = stash;
        this.value = value;
        this.targetId = targetId;
    }

    public Map<String, ?> getStash() {
        return stash;
    }

    public T getValue() {
        return value instanceof InputStreamReplicator isr ?
                (T) isr.replicate() :
                value;
    }

    public String getTargetId() {
        return targetId;
    }
}

package b.nana.technology.gingester.core.controller;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public final class ContextMap<T> {

    // TODO there might be an optimization possible for the common case where there is only one entry and
    // TODO that entry is for the __seed__ context. E.g. in `put` something like
    // TODO `seedOptimization = context.isSeed() && values.isEmpty()`, keep a second reference to the seedValue
    // TODO Entry outside of `values`.

    private final ConcurrentHashMap<Context, Entry<T>> values = new ConcurrentHashMap<>();
    private final ThreadLocal<Entry<T>> locked = new ThreadLocal<>();

    public void put(Context context, T value) {
        if (!context.isSynced()) throw new IllegalArgumentException("Given context is not a synced context");
        Object collision = values.put(context, new Entry<>(value));
        if (collision != null) throw new IllegalStateException("ContextMap already contains value for " + context);
    }

    private Entry<T> getEntry(Context context) {
        for (Context c : context) {
            if (c.isSynced()) {
                Entry<T> entry = values.get(c);
                if (entry != null) return entry;
            }
        }
        throw new IllegalStateException("ContextMap has no value for " + context);
    }

    public T get(Context context) {
        return getEntry(context).value;
    }

    public void act(Context context, Action<T> action) throws Exception {
        Entry<T> entry = getEntry(context);
        entry.lock.lock();
        try {
            action.perform(entry.value);
        } finally {
            entry.lock.unlock();
        }
    }

    public <V> V apply(Context context, Function<T, V> function) throws Exception {
        Entry<T> entry = getEntry(context);
        entry.lock.lock();
        try {
            return function.perform(entry.value);
        } finally {
            entry.lock.unlock();
        }
    }

    public T remove(Context context) {
        Entry<T> entry = values.remove(context);
        if (entry == null) throw new IllegalStateException("ContextMap has no value for " + context);
        return entry.value;
    }

    public void lock(Context context) {
        Entry<T> entry = getEntry(context);
        entry.lock.lock();
        locked.set(entry);
    }

    public T getLocked() {
        return locked.get().value;
    }

    public void unlock() {
        Entry<T> entry = locked.get();
        entry.lock.unlock();
        locked.remove();
    }

    public interface Action<T> {
        void perform(T value) throws Exception;
    }

    public interface Function<T, V> {
        V perform(T value) throws Exception;
    }

    private static class Entry<T> {

        private final ReentrantLock lock = new ReentrantLock();
        private final T value;

        private Entry(T value) {
            this.value = value;
        }
    }
}

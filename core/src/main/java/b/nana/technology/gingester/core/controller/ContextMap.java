package b.nana.technology.gingester.core.controller;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public final class ContextMap<T> {

    private final ConcurrentHashMap<Context, Entry<T>> values = new ConcurrentHashMap<>();
    private final ThreadLocal<Entry<T>> locked = new ThreadLocal<>();

    public void put(Context context, T value) {
        Object collision = values.put(context, new Entry<>(value));
        if (collision != null) throw new IllegalStateException("ContextMap already contains value for " + context);
    }

    public Entry<T> getEntry(Context context) {
        for (Context c : context) {
            Entry<T> entry = values.get(c);
            if (entry != null) return entry;
        }
        throw new IllegalStateException("ContextMap has no value for  " + context);
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

    public static class Entry<T> {

        public final ReentrantLock lock = new ReentrantLock();
        public final T value;

        private Entry(T value) {
            this.value = value;
        }
    }
}

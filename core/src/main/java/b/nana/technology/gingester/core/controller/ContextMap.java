package b.nana.technology.gingester.core.controller;

import java.util.concurrent.ConcurrentHashMap;

public final class ContextMap<T> {

    private final ConcurrentHashMap<Context, T> values = new ConcurrentHashMap<>();

    public void put(Context context, T value) {
        Object collision = values.put(context, value);
        if (collision != null) throw new IllegalStateException("ContextMap already contains value for " + context);
    }

    public T get(Context context) {
        for (Context c : context) {
            T value = values.get(c);
            if (value != null) return value;
        }
        throw new IllegalStateException("ContextMap has no value for  " + context);
    }

    public void act(Context context, Action<T> action) throws Exception {
        T value = get(context);
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (value) {
            action.perform(value);
        }
    }

    public T remove(Context context) {
        T value = values.remove(context);
        if (value == null) throw new IllegalStateException("ContextMap has no value for " + context);
        return value;
    }

    public interface Action<T> {
        void perform(T value) throws Exception;
    }
}

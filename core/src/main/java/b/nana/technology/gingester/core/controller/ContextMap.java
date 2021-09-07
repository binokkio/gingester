package b.nana.technology.gingester.core.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class ContextMap<T> {

    private final Map<Context, Supplier<T>> suppliers = new ConcurrentHashMap<>();
    private final Map<Thread, Map<Context, T>> values = new ConcurrentHashMap<>();

    public void put(Context context, Supplier<T> supplier) {
        Object collision = suppliers.put(context, supplier);
        if (collision != null) throw new IllegalStateException("ContextMap already contains value for " + context);
    }

    public T get(Context context) {
        for (Context c : context) {
            Supplier<T> supplier = suppliers.get(c);
            if (supplier != null) {
                Map<Context, T> map = values.computeIfAbsent(Thread.currentThread(), x -> new ConcurrentHashMap<>());
                return map.computeIfAbsent(c, x -> supplier.get());
            }
        }
        throw new IllegalStateException("No value for context from " + context.controller.id);
    }

    public Stream<T> remove(Context context) {
        Supplier<T> supplier = suppliers.remove(context);
        if (supplier == null) throw new IllegalStateException("ContextMap has no value for " + context);
        List<T> ts = new ArrayList<>();
        for (Map<Context, T> map : values.values()) {
            T t = map.remove(context);
            if (t != null) ts.add(t);
        }
        if (ts.isEmpty()) ts.add(supplier.get());
        return ts.stream();
    }
}

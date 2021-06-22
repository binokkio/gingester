package b.nana.technology.gingester.core;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ContextMap<T> {

    private final ConcurrentHashMap<Context, T> map = new ConcurrentHashMap<>();

    public void put(Context context, T t) {
        map.put(context, t);
    }

    public Optional<T> get(Context context) {
        for (Context c : context) {
            T t = map.get(c);
            if (t != null) return Optional.of(t);
        }
        return Optional.empty();
    }

    public T require(Context context) {
        return get(context).orElseThrow();
    }
}

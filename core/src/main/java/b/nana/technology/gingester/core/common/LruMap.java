package b.nana.technology.gingester.core.common;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class LruMap<K, V> extends LinkedHashMap<K, V> {

    private final int maxSize;
    private final BiConsumer<K, V> onExpelLru;

    public LruMap(int maxSize) {
        this(maxSize, null);
    }

    public LruMap(int maxSize, BiConsumer<K, V> onExpelLru) {
        super(16, .75F, true);
        this.maxSize = maxSize;
        this.onExpelLru = onExpelLru;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        if (size() > maxSize) {
            onExpelLru.accept(eldest.getKey(), eldest.getValue());
            return true;
        } else {
            return false;
        }
    }
}

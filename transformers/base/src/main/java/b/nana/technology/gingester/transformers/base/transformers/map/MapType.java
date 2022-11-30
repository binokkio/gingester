package b.nana.technology.gingester.transformers.base.transformers.map;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;
import java.util.function.Supplier;

public enum MapType {

    @JsonProperty("hash")
    HASH_MAP(HashMap::new),

    @JsonProperty("linked")
    LINKED_HASH_MAP(LinkedHashMap::new),

    @JsonProperty("tree")
    TREE_MAP(TreeMap::new);

    private final Supplier<Map<Object, Object>> mapSupplier;

    MapType(Supplier<Map<Object, Object>> mapSupplier) {
        this.mapSupplier = mapSupplier;
    }

    public <K, V> Map<K, V> newMap() {
        // noinspection unchecked
        return (Map<K, V>) mapSupplier.get();
    }
}

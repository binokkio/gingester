package b.nana.technology.gingester.transformers.base.transformers.set;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

public enum SetType {

    @JsonProperty("hash")
    HASH_SET(HashSet::new),

    @JsonProperty("linked")
    LINKED_HASH_SET(LinkedHashSet::new),

    @JsonProperty("tree")
    TREE_SET(TreeSet::new);

    private final Supplier<Set<Object>> setSupplier;

    SetType(Supplier<Set<Object>> setSupplier) {
        this.setSupplier = setSupplier;
    }

    public <T> Set<T> newSet() {
        // noinspection unchecked
        return (Set<T>) setSupplier.get();
    }
}

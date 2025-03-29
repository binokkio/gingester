package b.nana.technology.gingester.transformers.base.transformers.list;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public enum ListType {

    @JsonProperty("array")
    ARRAY_LIST(ArrayList::new),

    @JsonProperty("linked")
    LINKED_LIST(LinkedList::new);

    private final Supplier<List<Object>> listSupplier;

    ListType(Supplier<List<Object>> listSupplier) {
        this.listSupplier = listSupplier;
    }

    public <T> List<T> newList() {
        // noinspection unchecked
        return (List<T>) listSupplier.get();
    }
}

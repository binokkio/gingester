package b.nana.technology.gingester.core.transformer;

import b.nana.technology.gingester.core.controller.FetchKey;

import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class StashDetails {

    /**
     * Create StashDetails for the given name and type and mark that entry for ordinal fetch inclusion.
     */
    public static StashDetails ofOrdinal(String name, Object type) {
        return new StashDetails(Map.of(name, type)).ordinal(name);
    }

    public static StashDetails of() {
        return new StashDetails(Map.of());
    }

    public static StashDetails of(String name, Object type) {
        return new StashDetails(Map.of(name, type));
    }

    public static StashDetails of(String name1, Object type1, String name2, Object type2) {
        return new StashDetails(Map.of(name1, type1, name2, type2));
    }

    public static StashDetails of(String name1, Object type1, String name2, Object type2, String name3, Object type3) {
        return new StashDetails(Map.of(name1, type1, name2, type2, name3, type3));
    }

    public static StashDetails of(Map<String, Object> types) {
        return new StashDetails(types);
    }

    private final Map<String, Object> types;
    private FetchKey ordinal;

    private StashDetails(Map<String, Object> types) {
        this.types = requireNonNull(types);
    }

    /**
     * Mark the entry at the given key for inclusion in ordinal fetches.
     *
     * @param key the key of the entry to mark
     * @return this StashDetails
     */
    public StashDetails ordinal(String key) {

        if (ordinal != null)
            throw new IllegalStateException("An entry was already marked for ordinal fetch inclusion");

        ordinal = new FetchKey(key);
        return this;
    }

    public Map<String, Object> getTypes() {
        return types;
    }

    public Optional<FetchKey> getOrdinal() {
        return Optional.ofNullable(ordinal);
    }
}

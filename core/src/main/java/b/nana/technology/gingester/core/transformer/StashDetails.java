package b.nana.technology.gingester.core.transformer;

import b.nana.technology.gingester.core.controller.FetchKey;

import java.util.Map;
import java.util.Optional;

public final class StashDetails {

    public static StashDetails ofExplicit(String name, Object type) {
        return new StashDetails(Map.of(name, type)).markExplicit(name);
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
    private FetchKey explicit;

    private StashDetails(Map<String, Object> types) {
        this.types = types;
    }

    public StashDetails markExplicit(String explicit) {
        this.explicit = new FetchKey(explicit);
        return this;
    }

    public Map<String, Object> getTypes() {
        return types;
    }

    public Optional<FetchKey> getExplicit() {
        return Optional.ofNullable(explicit);
    }
}

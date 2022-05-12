package b.nana.technology.gingester.core.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public final class FetchKey {

    private final boolean isOrdinal;
    private final int ordinal;
    private final String[] names;

    @JsonCreator
    public FetchKey(String fetch) {
        if (fetch.charAt(0) == '^') {
            isOrdinal = true;
            ordinal = Integer.parseInt(fetch.substring(1));
            names = null;
        } else {
            names = fetch.split("\\.");
            isOrdinal = false;
            ordinal = 0;
        }
    }

    public FetchKey(String fetch, boolean isSingleName) {
        isOrdinal = false;
        ordinal = 0;
        names = new String[] { fetch };
    }

    public boolean isOrdinal() {
        return isOrdinal;
    }

    public int ordinal() {
        return ordinal;
    }

    public FetchKey decrement() {
        if (!isOrdinal) {
            throw new IllegalStateException("decrement() called on non-ordinal fetch key");
        }
        return new FetchKey("^" + (ordinal - 1));
    }

    public boolean hasNames() {
        return !isOrdinal;
    }

    public String[] getNames() {
        if (isOrdinal) {
            throw new IllegalStateException("getNames() called on ordinal fetch key");
        }
        return names;
    }

    @Override
    @JsonValue
    public String toString() {
        return isOrdinal ? "^" + ordinal : String.join(".", names);
    }
}

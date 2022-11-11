package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.Id;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public final class FetchKey {

    private static final String[] EMPTY_NAMES = new String[0];

    private final boolean isOrdinal;
    private final int ordinal;
    private final String id;
    private final boolean isLocalId;
    private final String[] names;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public FetchKey(String fetch) {
        if (fetch.isEmpty()) {
            throw new IllegalArgumentException("Empty fetch key");
        } else if (fetch.charAt(0) == '^') {
            isOrdinal = true;
            ordinal = fetch.length() == 1 ? 1 : Integer.parseInt(fetch.substring(1));
            id = null;
            isLocalId = false;
            names = null;
        } else {
            isOrdinal = false;
            ordinal = 0;
            Target target = new Target(fetch);
            id = target.id;
            isLocalId = target.isLocalId;
            names = target.names;
        }
    }

    public FetchKey(String fetch, boolean isSingleName) {
        if (isSingleName) {
            isOrdinal = false;
            ordinal = 0;
            if (fetch.charAt(0) == Id.SCOPE_DELIMITER) {
                id = fetch;
                isLocalId = false;
                names = EMPTY_NAMES;
            } else if (fetch.charAt(0) >= 'A' && fetch.charAt(0) <= 'Z') {
                id = fetch;
                isLocalId = true;
                names = EMPTY_NAMES;
            } else {
                id = null;
                isLocalId = false;
                names = new String[] { fetch };
            }
        } else {
            throw new IllegalArgumentException("Use FetchKey(String) constructor instead");
        }
    }

    public FetchKey(int ordinal) {
        this.isOrdinal = true;
        this.ordinal = ordinal;
        this.id = null;
        this.isLocalId = false;
        this.names = null;
    }

    public boolean isOrdinal() {
        return isOrdinal;
    }

    public int ordinal() {
        return ordinal;
    }

    public FetchKey decrement() {
        return new FetchKey(ordinal - 1);
    }

    public boolean hasTarget() {
        return id != null;
    }

    public boolean matchesTarget(Id id) {
        return isLocalId ?
                id.getLocalId().equals(this.id) :
                id.getGlobalId().equals(this.id);
    }

    public String[] getNames() {
        return names;
    }

    @Override
    @JsonValue
    public String toString() {
        if (isOrdinal) {
            return ordinal == 1 ? "^" : "^" + ordinal;
        } else if (id == null) {
            return String.join(".", names);
        } else {
            if (names.length == 0) {
                return id;
            } else {
                return id + '.' + String.join(".", names);
            }
        }
    }

    private static class Target {

        Target(String fetch) {

            if (fetch.contains(".."))
                throw new IllegalArgumentException("Fetch keys don't support scope-up references");

            String[] parts = fetch.split("\\.");

            if (parts[0].charAt(0) == Id.SCOPE_DELIMITER) {
                id = parts[0];
                isLocalId = false;
                names = new String[parts.length - 1];
                System.arraycopy(parts, 1, names, 0, names.length);
            } else if (parts[0].charAt(0) >= 'A' && parts[0].charAt(0) <= 'Z') {
                id = parts[0];
                isLocalId = true;
                names = new String[parts.length - 1];
                System.arraycopy(parts, 1, names, 0, names.length);
            } else {
                id = null;
                isLocalId = false;
                names = parts;
            }
        }

        final String id;
        final boolean isLocalId;
        final String[] names;
    }
}

package b.nana.technology.gingester.core.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public final class FetchKey {

    private static final String[] EMPTY_NAMES = new String[0];

    private final boolean isOrdinal;
    private final int ordinal;
    private final String target;
    private final String[] names;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public FetchKey(String fetch) {
        if (fetch.isEmpty()) {
            throw new IllegalArgumentException("Empty fetch key");
        } else if (fetch.charAt(0) == '^') {
            isOrdinal = true;
            ordinal = fetch.length() == 1 ? 1 : Integer.parseInt(fetch.substring(1));
            target = null;
            names = null;
        } else {
            isOrdinal = false;
            ordinal = 0;
            String[] parts = fetch.split("\\.");
            if (isTarget(parts[0])) {
                target = parts[0];
                names = new String[parts.length - 1];
                System.arraycopy(parts, 1, names, 0, names.length);
            } else {
                target = null;
                names = parts;
            }
        }
    }

    public FetchKey(String fetch, boolean isSingleName) {
        if (isSingleName) {
            isOrdinal = false;
            ordinal = 0;
            if (isTarget(fetch)) {
                target = fetch;
                names = EMPTY_NAMES;
            } else {
                target = null;
                names = new String[] { fetch };
            }
        } else {
            throw new IllegalArgumentException("Use FetchKey(String) constructor instead");
        }
    }

    public FetchKey(int ordinal) {
        this.isOrdinal = true;
        this.ordinal = ordinal;
        this.target = null;
        this.names = null;
    }

    private boolean isTarget(String test) {
        return test.charAt(0) >= 'A' && test.charAt(0) <= 'Z';
    }

    public boolean isOrdinal() {
        return isOrdinal;
    }

    public int ordinal() {
        return ordinal;
    }

    public FetchKey decrement() {
        return new FetchKey("^" + (ordinal - 1));
    }

    public boolean hasTarget() {
        return target != null;
    }

    public String getTarget() {
        return target;
    }

    public String[] getNames() {
        return names;
    }

    @Override
    @JsonValue
    public String toString() {
        return isOrdinal ?
                ordinal == 1 ? "^" : "^" + ordinal :
                String.join(".", names);
    }
}

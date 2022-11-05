package b.nana.technology.gingester.core;

import java.util.regex.Pattern;

// Note that this class does not override equals or hashcode but its instances are used as Map keys and in Sets,
// the IdFactory is there to make sure only one instance exists for a global id string.

public final class Id {

    public static final Id SEED = new Id("$__seed__");
    public static final Id ELOG = new Id("$__elog__");

    public static final Pattern ID_PART = Pattern.compile("[_A-Z][_A-Za-z0-9]*");
    public static final char SCOPE_DELIMITER = '$';
    public static final String SCOPE_UP = "..";

    public static Id newTestId(String id) {
        return new Id(id);
    }

    private final String globalId;
    private final String localId;

    Id(String globalId) {

        if (globalId.charAt(0) != SCOPE_DELIMITER)
            throw new IllegalArgumentException("\"" + globalId + "\" is not a global id");

        this.globalId = globalId;

        String[] parts = globalId.split("\\" + SCOPE_DELIMITER);
        this.localId = parts[parts.length - 1];
    }

    public String getGlobalId() {
        return globalId;
    }

    public String getLocalId() {
        return localId;
    }

    @Override
    public String toString() {
        return globalId.substring(1);
    }
}

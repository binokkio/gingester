package b.nana.technology.gingester.core;

import java.util.*;
import java.util.stream.Collectors;

public final class IdFactory {

    private final Map<String, Id> ids = new HashMap<>();

    public IdFactory() {
        ids.put(Id.SEED.getGlobalId(), Id.SEED);
        ids.put(Id.ELOG.getGlobalId(), Id.ELOG);
    }

    public Id getId(String id) {
        return getId(id, Collections.emptyList());
    }

    public Id getId(String id, Collection<String> scopes) {

        String localId;
        Deque<String> idParts;

        if (id.charAt(0) == Id.SCOPE_DELIMITER) {
            localId = id.substring(1);
            idParts = new ArrayDeque<>();
        } else {
            localId = id;
            idParts = new ArrayDeque<>(scopes);
        }

        for (String idPart : localId.split("\\" + Id.SCOPE_DELIMITER)) {
            if (idPart.equals(Id.SCOPE_UP)) {
                if (idParts.isEmpty())
                    throw new IllegalArgumentException("\"" + localId + "\" has too many up references (\"" + Id.SCOPE_UP + "\")");
                idParts.removeLast();
            } else {
                if (!Id.ID_PART.matcher(idPart).matches())
                    throw new IllegalArgumentException("\"" + localId + "\" contains invalid part \"" + idPart + "\", must match " + Id.ID_PART.pattern());
                idParts.addLast(idPart);
            }
        }

        String globalId = Id.SCOPE_DELIMITER + String.join("" + Id.SCOPE_DELIMITER, idParts);

        return ids.computeIfAbsent(globalId, Id::new);
    }

    public List<Id> getIds(Collection<String> ids) {
        return getIds(ids, Collections.emptyList());
    }

    public List<Id> getIds(Collection<String> ids, Collection<String> scopes) {
        return ids.stream().map(id -> getId(id, scopes)).collect(Collectors.toList());
    }

    public String getGlobalId(String id, Collection<String> scopes) {
        return getId(id, scopes).getGlobalId();
    }

    public List<String> getGlobalIds(Collection<String> ids, Collection<String> scopes) {
        return ids.stream().map(id -> getGlobalId(id, scopes)).collect(Collectors.toList());
    }
}

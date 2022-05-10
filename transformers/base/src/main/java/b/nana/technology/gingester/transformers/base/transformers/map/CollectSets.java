package b.nana.technology.gingester.transformers.base.transformers.map;

import java.util.HashSet;
import java.util.Set;

public final class CollectSets extends CollectCollections<Set<Object>> {

    public CollectSets(Parameters parameters) {
        super(parameters);
    }

    @Override
    protected Set<Object> supply() {
        return new HashSet<>();
    }

    @Override
    protected void add(Set<Object> collection, Object value) {
        collection.add(value);
    }

    @Override
    protected Set<Object> reduce(Set<Object> a, Set<Object> b) {
        if (a.size() > b.size()) {
            a.addAll(b);
            return a;
        } else {
            b.addAll(a);
            return b;
        }
    }
}

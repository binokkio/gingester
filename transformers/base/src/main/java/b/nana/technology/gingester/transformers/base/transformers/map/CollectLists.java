package b.nana.technology.gingester.transformers.base.transformers.map;

import java.util.ArrayList;
import java.util.List;

public final class CollectLists extends CollectCollections<List<Object>> {

    public CollectLists(Parameters parameters) {
        super(parameters);
    }

    @Override
    protected List<Object> supply() {
        return new ArrayList<>();
    }

    @Override
    protected void add(List<Object> collection, Object value) {
        collection.add(value);
    }

    @Override
    protected List<Object> reduce(List<Object> a, List<Object> b) {
        if (a.size() > b.size()) {
            a.addAll(b);
            return a;
        } else {
            b.addAll(a);
            return b;
        }
    }
}

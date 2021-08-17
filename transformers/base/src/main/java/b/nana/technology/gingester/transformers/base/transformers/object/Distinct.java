package b.nana.technology.gingester.transformers.base.transformers.object;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.ContextMap;
import b.nana.technology.gingester.core.Passthrough;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.HashSet;
import java.util.Set;

public class Distinct<T> extends Passthrough<T> {

    private final ContextMap<Set<T>> contextMap = new ContextMap<>();
    private final boolean sort;

    public Distinct(Parameters parameters) {
        super(parameters);
        sort = parameters.sort;
    }

    @Override
    protected void prepare(Context context) {
        contextMap.put(context, new HashSet<>());
    }

    @Override
    protected void transform(Context context, T input) throws Exception {
        contextMap.require(context).add(input);
    }

    @Override
    protected void finish(Context context) {
        if (sort) {
            contextMap.requireRemove(context)
                    .stream()
                    .sorted()
                    .forEach(object -> emit(context, object));
        } else {
            contextMap.requireRemove(context)
                    .forEach(object -> emit(context, object));
        }
    }

    public static class Parameters {

        public boolean sort;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(boolean sort) {
            this.sort = sort;
        }
    }
}

package b.nana.technology.gingester.core;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.HashMap;
import java.util.Map;

public final class Stash<T> extends Passthrough<T> {

    private final String key;
    private final boolean weak;

    public Stash() {
        this(new Parameters());
    }

    public Stash(Parameters parameters) {
        super(parameters);
        key = parameters.key;
        weak = parameters.weak;
    }

    @Override
    protected void transform(Context context, T input) throws Exception {

        Map<String, Object> stash = weak ?
                createWeakMapFor(key, input) :
                Map.of(key, input);

        emit(
                context.extend(this).stash(stash),
                input
        );
    }

    private Map<String, Object> createWeakMapFor(String key, T value) {
        Map<String, Object> map = new HashMap<>(1);
        map.put(key, value);
        return map;
    }

    public static class Parameters {

        public String key = "stash";
        public boolean weak;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String key) {
            this.key = key;
        }
    }
}

package b.nana.technology.gingester.core;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;

public class Stash<T> extends Passthrough<T> {

    private final String key;
    private final int ttl;

    public Stash(Parameters parameters) {
        super(parameters);
        key = parameters.key;
        ttl = parameters.ttl;
    }

    @Override
    protected void transform(Context context, T input) throws Exception {
        emit(context.extend(this).details(Map.of(key, new Item(input, ttl))), input);
    }

    public static class Parameters {

        public String key = "stash";
        public int ttl = 1;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String key) {
            this.key = key;
        }
    }

    static class Item {

        private Object value;
        private int ttl;

        private Item(Object value, int ttl) {
            this.value = value;
            this.ttl = ttl;
        }

        synchronized Object get() {
            if (value == null) throw new IllegalStateException("Expired");  // TODO
            Object returnValue = value;
            if (ttl != -1 && --ttl == 0) value = null;
            return returnValue;
        }
    }
}

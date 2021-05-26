package b.nana.technology.gingester.core;

import com.fasterxml.jackson.annotation.JsonCreator;

public class Fetch<T> extends Transformer<Object, T> {

    private final String key;

    public Fetch(Parameters parameters) {
        super(parameters);
        key = parameters.key;
    }

    @Override
    protected void transform(Context context, Object input) throws Exception {
        Stash.Item item = (Stash.Item) context.getDetail(key).orElseThrow();
        emitUnchecked(context, item.get());
    }

    public static class Parameters {

        public String key = "stash";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String key) {
            this.key = key;
        }
    }
}

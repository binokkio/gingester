package b.nana.technology.gingester.core;

import com.fasterxml.jackson.annotation.JsonCreator;

public final class Fetch<T> extends Transformer<Object, T> {

    private final String key;
    private final boolean clear;

    public Fetch() {
        this(new Parameters());
    }

    public Fetch(Parameters parameters) {
        super(parameters);
        key = parameters.key;
        clear = parameters.clear;
    }

    @Override
    protected void transform(Context context, Object input) throws Exception {
        emitUnchecked(
                context,
                clear ?
                        context.clear(key).orElseThrow() :
                        context.fetch(key).orElseThrow()
        );
    }

    public static class Parameters {

        public String key = "stash";
        public boolean clear;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String key) {
            this.key = key;
        }
    }
}

package b.nana.technology.gingester.core;

import com.fasterxml.jackson.annotation.JsonCreator;

public final class Fetch<T> extends Transformer<Object, T> {

    private final String[] stashName;

    public Fetch() {
        this(new Parameters());
    }

    public Fetch(Parameters parameters) {
        super(parameters);
        stashName = parameters.stash.split("\\.");
    }

    @Override
    protected void transform(Context context, Object input) throws Exception {
        emitUnchecked(
                context,
                context.fetch(stashName).orElseThrow()
        );
    }

    public static class Parameters {

        public String stash = "stash";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String stash) {
            this.stash = stash;
        }
    }
}

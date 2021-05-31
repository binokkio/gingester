package b.nana.technology.gingester.core;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;

public final class Swap<T, U> extends Transformer<T, U> {

    private final String stash;

    public Swap() {
        this(new Parameters());
    }

    public Swap(Parameters parameters) {
        super(parameters);
        stash = parameters.stash;
        if (stash.contains(".")) throw new IllegalArgumentException("'.' not allowed in stash name");  // TODO
    }

    @Override
    protected void transform(Context context, T input) throws Exception {
        emitUnchecked(
                context.extend(this).description("swap " + stash).stash(Map.of(stash, input)),
                context.fetch(stash).orElseThrow()
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

package b.nana.technology.gingester.core;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;

public final class Stash<T> extends Passthrough<T> {

    private final String stash;
    private final Object value;

    public Stash() {
        this(new Parameters());
    }

    public Stash(Parameters parameters) {
        super(parameters);
        stash = parameters.stash;
        value = parameters.value;
        if (stash.contains(".")) throw new IllegalArgumentException("'.' not allowed in stash name");  // TODO
    }

    @Override
    protected void transform(Context context, T input) throws Exception {
        emit(
                context.extend(this).description("stash " + stash)
                        .stash(Map.of(stash, value != null ? value : input)),
                input
        );
    }

    public static class Parameters {

        public String stash = "stash";
        public Object value;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String stash) {
            this.stash = stash;
        }
    }
}

package b.nana.technology.gingester.transformers.base.transformers;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;

public class Stash extends Transformer<Object, Void> {

    private final String key;

    public Stash(Parameters parameters) {
        super(parameters);
        key = parameters.key;
    }

    @Override
    protected void transform(Context context, Object input) throws Exception {
        emit(context.extend(this).details(Map.of(key, input)), null);
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

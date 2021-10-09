package b.nana.technology.gingester.transformers.redis;

import b.nana.technology.gingester.core.controller.Context;
import com.fasterxml.jackson.annotation.JsonCreator;

public abstract class KeyTransformer<I, O> extends RedisTransformer<I, O> {

    private final Context.Template keyFormat;

    public KeyTransformer(Parameters parameters) {
        super(parameters);
        keyFormat = Context.newTemplate(parameters.keyFormat);
    }

    protected final Context.Template getKeyFormat() {
        return keyFormat;
    }

    public static class Parameters extends RedisTransformer.Parameters {

        public String keyFormat;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String keyFormat) {
            this.keyFormat = keyFormat;
        }
    }
}

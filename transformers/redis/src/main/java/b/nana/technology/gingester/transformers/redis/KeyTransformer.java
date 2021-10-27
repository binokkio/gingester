package b.nana.technology.gingester.transformers.redis;

import b.nana.technology.gingester.core.controller.Context;
import com.fasterxml.jackson.annotation.JsonCreator;

public abstract class KeyTransformer<I, O> extends RedisTransformer<I, O> {

    private final Context.Template keyTemplate;

    public KeyTransformer(Parameters parameters) {
        super(parameters);
        keyTemplate = Context.newTemplate(parameters.key);
    }

    protected final Context.Template getKeyTemplate() {
        return keyTemplate;
    }

    public static class Parameters extends RedisTransformer.Parameters {

        public String key;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String key) {
            this.key = key;
        }
    }
}

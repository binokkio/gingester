package b.nana.technology.gingester.transformers.redis;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import com.fasterxml.jackson.annotation.JsonCreator;

public abstract class KeyTransformer<I, O> extends RedisTransformer<I, O> {

    private final Template keyTemplate;

    public KeyTransformer(Parameters parameters) {
        super(parameters);
        keyTemplate = Context.newTemplate(parameters.key);
    }

    protected final Template getKeyTemplate() {
        return keyTemplate;
    }

    public static class Parameters extends RedisTransformer.Parameters {

        public TemplateParameters key;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(TemplateParameters key) {
            this.key = key;
        }
    }
}

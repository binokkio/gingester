package b.nana.technology.gingester.transformers.base.transformers;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class Tag extends Transformer<Object, Object> {  // TODO PassthroughTransformer

    private final String tag;

    public Tag(Parameters parameters) {
        super(parameters);
        tag = parameters.tag;
    }

    @Override
    protected void transform(Context context, Object input) throws Exception {
        emit(context.extend(this).attachment(tag), input);  // TODO also description?
    }

    public static class Parameters {

        @JsonValue
        public String tag;

        @JsonCreator
        public Parameters(String tag) {
            this.tag = tag;
        }
    }
}

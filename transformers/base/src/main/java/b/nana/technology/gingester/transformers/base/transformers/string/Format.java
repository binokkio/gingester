package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

public class Format extends Transformer<Object, String> {

    private final Context.StringFormat format;

    public Format(Parameters parameters) {
        super(parameters);
        format = new Context.StringFormat(parameters.format);
    }

    @Override
    protected void transform(Context context, Object input) {
        emit(context, format.format(context));
    }

    public static class Parameters {

        public String format;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String format) {
            this.format = format;
        }
    }
}

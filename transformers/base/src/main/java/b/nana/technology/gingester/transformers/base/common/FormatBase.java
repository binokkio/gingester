package b.nana.technology.gingester.transformers.base.common;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

public abstract class FormatBase<T> extends Transformer<Object, T> {

    private final Context.StringFormat format;

    public FormatBase(Parameters parameters) {
        super(parameters);
        format = new Context.StringFormat(parameters.format);
    }

    protected final Context.StringFormat getFormat() {
        return format;
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

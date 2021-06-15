package b.nana.technology.gingester.transformers.base.transformers.resource;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.InputStream;

public class Open extends Transformer<Object, InputStream> {

    private final Context.StringFormat resourceFormat;

    public Open(Parameters parameters) {
        super(parameters);
        resourceFormat = new Context.StringFormat(parameters.resourceFormat);
    }

    @Override
    protected void transform(Context context, Object input) throws Exception {
        String resourceString = resourceFormat.format(context);
        emit(
                context.extend(this).description(resourceString),
                getClass().getResourceAsStream(resourceFormat.format(context))
        );
    }

    public static class Parameters {

        public String resourceFormat;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String resourceFormat) {
            this.resourceFormat = resourceFormat;
        }
    }
}

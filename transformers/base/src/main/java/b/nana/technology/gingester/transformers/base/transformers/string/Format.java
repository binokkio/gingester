package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.transformers.base.common.FormatBase;

public class Format extends FormatBase<String> {

    public Format(Parameters parameters) {
        super(parameters);
    }

    @Override
    protected void transform(Context context, Object input) throws Exception {
        emit(context, getFormat().format(context));
    }
}

package b.nana.technology.gingester.transformers.base.transformers.object;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;

public class ToString extends Transformer<Object, String> {

    @Override
    protected void transform(Context context, Object input) throws Exception {
        emit(context, input.toString());
    }
}

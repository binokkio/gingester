package b.nana.technology.gingester.test.transformers;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;

public class ExceptionHandler extends Transformer<Throwable, String> {

    @Override
    protected void transform(Context context, Throwable input) {
        emit(context.extend(this), input.getMessage());
    }
}

package b.nana.technology.gingester.test.transformers;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;

public class Emphasize extends Transformer<String, String> {

    @Override
    protected void transform(Context context, String input) {
        emit(context.extend(this), input + '!');
    }
}

package b.nana.technology.gingester.test.transformers;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Passthrough;
import b.nana.technology.gingester.core.Transformer;

public class ExceptionThrower extends Passthrough<String> {

    @Override
    protected void transform(Context context, String input) {
        throw new RuntimeException("ExceptionThrower throws");
    }
}

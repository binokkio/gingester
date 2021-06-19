package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Passthrough;

import java.io.IOException;
import java.io.InputStream;

public class Drain extends Passthrough<InputStream> {

    @Override
    protected void transform(Context context, InputStream input) throws IOException {
        input.skip(Long.MAX_VALUE);
        emit(context, input);
    }
}

package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ToInputStream extends Transformer<String, InputStream> {

    @Override
    protected void setup(Setup setup) {
        setup.syncInputs();
    }

    @Override
    protected void transform(Context context, String input) throws Exception {
        emit(context, new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
    }
}

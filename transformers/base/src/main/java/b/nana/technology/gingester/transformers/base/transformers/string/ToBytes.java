package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;

import java.nio.charset.StandardCharsets;

public class ToBytes extends Transformer<String, byte[]> {
    @Override
    protected void transform(Context context, String input) throws Exception {
        emit(context, input.getBytes(StandardCharsets.UTF_8));
    }
}

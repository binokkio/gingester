package b.nana.technology.gingester.transformers.base.transformers.bytes;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;

import java.io.IOException;
import java.io.InputStream;

public class ToString extends Transformer<byte[], String> {

    @Override
    protected void transform(Context context, byte[] input) {
        emit(context, new String(input));
    }
}

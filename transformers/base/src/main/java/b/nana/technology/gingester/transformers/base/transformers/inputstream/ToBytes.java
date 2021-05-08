package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;

import java.io.IOException;
import java.io.InputStream;

public class ToBytes extends Transformer<InputStream, byte[]> {

    @Override
    protected void transform(Context context, InputStream input) throws IOException {
        emit(context, input.readAllBytes());
    }
}

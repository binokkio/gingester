package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.transformers.base.common.string.CharsetTransformer;

import java.io.InputStream;

@Pure
public final class ToString extends CharsetTransformer<InputStream, String> {

    public ToString(Parameters parameters) {
        super(parameters);
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<String> out) throws Exception {
        out.accept(context, new String(in.readAllBytes(), getCharset()));
    }
}

package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.transformers.base.common.string.CharsetTransformer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Pure
public final class ToInputStream extends CharsetTransformer<String, InputStream> {

    public ToInputStream(Parameters parameters) {
        super(parameters);
    }

    @Override
    public void transform(Context context, String in, Receiver<InputStream> out) throws Exception {
        out.accept(context, new ByteArrayInputStream(in.getBytes(getCharset())));
    }
}

package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.transformers.base.common.string.CharsetTransformer;

@Pure
public final class ToBytes extends CharsetTransformer<String, byte[]> {

    public ToBytes(Parameters parameters) {
        super(parameters);
    }

    @Override
    public void transform(Context context, String in, Receiver<byte[]> out) throws Exception {
        out.accept(context, in.getBytes(getCharset()));
    }
}

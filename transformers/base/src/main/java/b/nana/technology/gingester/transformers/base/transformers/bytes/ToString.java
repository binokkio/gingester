package b.nana.technology.gingester.transformers.base.transformers.bytes;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.transformers.base.common.string.CharsetTransformer;

public class ToString extends CharsetTransformer<byte[], String> {

    public ToString(Parameters parameters) {
        super(parameters);
    }

    @Override
    public void transform(Context context, byte[] in, Receiver<String> out) throws Exception {
        out.accept(context, new String(in, getCharset()));
    }
}

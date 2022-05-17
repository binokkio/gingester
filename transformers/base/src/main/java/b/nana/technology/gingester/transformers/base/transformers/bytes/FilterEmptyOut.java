package b.nana.technology.gingester.transformers.base.transformers.bytes;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

public final class FilterEmptyOut implements Transformer<byte[], byte[]> {

    @Override
    public void transform(Context context, byte[] in, Receiver<byte[]> out) throws Exception {
        if (in.length > 0) {
            out.accept(context, in);
        }
    }
}

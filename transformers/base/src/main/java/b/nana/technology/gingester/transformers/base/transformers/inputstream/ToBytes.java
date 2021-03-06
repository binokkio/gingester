package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.io.InputStream;

@Pure
public final class ToBytes implements Transformer<InputStream, byte[]> {

    @Override
    public void transform(Context context, InputStream in, Receiver<byte[]> out) throws Exception {
        out.accept(context, in.readAllBytes());
    }
}

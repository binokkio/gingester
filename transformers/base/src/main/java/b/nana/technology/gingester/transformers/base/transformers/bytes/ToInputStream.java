package b.nana.technology.gingester.transformers.base.transformers.bytes;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Pure
public final class ToInputStream implements Transformer<byte[], InputStream> {

    @Override
    public void transform(Context context, byte[] in, Receiver<InputStream> out) throws Exception {
        out.accept(context, new ByteArrayInputStream(in));
    }
}

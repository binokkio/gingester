package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.io.InputStream;

public final class Drain implements Transformer<InputStream, InputStream> {

    @Override
    public void transform(Context context, InputStream in, Receiver<InputStream> out) throws Exception {
        in.skip(Long.MAX_VALUE);
        out.accept(context, in);
    }
}

package b.nana.technology.gingester.transformers.base.transformers.std;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.io.InputStream;

public final class In implements Transformer<Object, InputStream> {

    @Override
    public void transform(Context context, Object in, Receiver<InputStream> out) {
        out.accept(context.stash("description", "stdin"), System.in);
    }
}

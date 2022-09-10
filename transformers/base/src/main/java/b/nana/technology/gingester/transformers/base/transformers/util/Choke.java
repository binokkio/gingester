package b.nana.technology.gingester.transformers.base.transformers.util;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

@Names(1)
@Passthrough
public final class Choke implements Transformer<Object, Object> {

    private final Object choke = new Object();

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        synchronized (choke) {
            out.accept(context, in);
        }
    }
}

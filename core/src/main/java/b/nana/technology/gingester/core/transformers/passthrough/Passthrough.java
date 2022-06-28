package b.nana.technology.gingester.core.transformers.passthrough;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

@Names(1)
@b.nana.technology.gingester.core.annotations.Passthrough
public final class Passthrough implements Transformer<Object, Object> {

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        out.accept(context, in);
    }
}

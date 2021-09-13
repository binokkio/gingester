package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

@Names(1)
public final class Seed implements Transformer<Void, Object> {

    @Override
    public void transform(Context context, Void in, Receiver<Object> out) {
        out.accept(context, null);
    }
}

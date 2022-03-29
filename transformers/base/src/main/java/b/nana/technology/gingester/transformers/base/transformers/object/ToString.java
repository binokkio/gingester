package b.nana.technology.gingester.transformers.base.transformers.object;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

@Names(1)
public final class ToString implements Transformer<Object, String> {

    @Override
    public void transform(Context context, Object in, Receiver<String> out) throws Exception {
        out.accept(context, in.toString());
    }
}

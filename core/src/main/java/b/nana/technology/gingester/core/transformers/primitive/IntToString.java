package b.nana.technology.gingester.core.transformers.primitive;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

@Pure
public final class IntToString implements Transformer<Integer, String> {

    @Override
    public void transform(Context context, Integer in, Receiver<String> out) throws Exception {
        out.accept(context, Integer.toString(in));
    }
}

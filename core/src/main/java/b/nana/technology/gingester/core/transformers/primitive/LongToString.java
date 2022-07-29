package b.nana.technology.gingester.core.transformers.primitive;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

@Pure
public final class LongToString implements Transformer<Long, String> {

    @Override
    public void transform(Context context, Long in, Receiver<String> out) throws Exception {
        out.accept(context, Long.toString(in));
    }
}

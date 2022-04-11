package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

@Names(1)
public final class OnPrepare implements Transformer<Object, Object> {

    private static final String PREPARE_SIGNAL = "prepare signal";

    @Override
    public void prepare(Context context, Receiver<Object> out) throws Exception {
        out.accept(context, PREPARE_SIGNAL);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {

    }
}

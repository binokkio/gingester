package b.nana.technology.gingester.test.transformers;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

public class SyncFinish implements Transformer<Object, String> {

    @Override
    public void transform(Context context, Object in, Receiver<String> out) {
        // ignore input, emit in finish()
    }

    @Override
    public void finish(Context context, Receiver<String> out) {
        out.accept(context, "Message from SyncFinish finish()");
    }
}

package b.nana.technology.gingester.test.transformers;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

public class Sync implements Transformer<String, String> {

    @Override
    public void transform(Context context, String in, Receiver<String> out) throws Exception {
        // ignore input, emit in finish()
    }

    @Override
    public void finish(Context context, Receiver<String> out) {
        out.accept(context, "Message from Sync finish()");
    }
}

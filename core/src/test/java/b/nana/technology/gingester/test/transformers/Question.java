package b.nana.technology.gingester.test.transformers;

import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;

public class Question implements Transformer<String, String> {

    @Override
    public void transform(Context context, String in, Receiver<String> out) {
        out.accept(context, in + '?');
    }
}

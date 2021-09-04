package b.nana.technology.gingester.test.transformers;

import b.nana.technology.gingester.core.Transformer;
import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.receiver.Receiver;

public class Emphasize implements Transformer<String, String> {

    @Override
    public void transform(Context context, String in, Receiver<String> out) {
        out.accept(context, in + '!');
    }
}

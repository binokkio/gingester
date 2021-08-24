package b.nana.technology.transformers.base;

import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

public class StringAppend implements Transformer<String, String> {

    @Override
    public void transform(Context context, String in, Receiver<String> out) {
        out.accept(context, in + "!");
    }
}

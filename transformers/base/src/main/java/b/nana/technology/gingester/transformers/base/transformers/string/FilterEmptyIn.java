package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

public final class FilterEmptyIn implements Transformer<String, String>  {

    @Override
    public void transform(Context context, String in, Receiver<String> out) {
        if (in.isEmpty()) {
            out.accept(context, in);
        }
    }
}

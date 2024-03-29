package b.nana.technology.gingester.transformers.base.transformers.std;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

@Passthrough
public final class Out implements Transformer<String, String> {

    @Override
    public void transform(Context context, String in, Receiver<String> out) {
        System.out.println(in);
        out.accept(context, in);
    }
}

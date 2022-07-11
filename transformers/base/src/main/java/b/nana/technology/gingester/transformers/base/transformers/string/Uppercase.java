package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.Locale;

public final class Uppercase implements Transformer<String, String> {

    @Override
    public void transform(Context context, String in, Receiver<String> out) {
        out.accept(context, in.toUpperCase(Locale.ROOT));  // TODO allow locale to be configured
    }
}

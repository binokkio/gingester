package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.regex.Matcher;

@Pure
public final class Group0 implements Transformer<Matcher, String> {

    @Override
    public void transform(Context context, Matcher in, Receiver<String> out) {
        out.accept(context, in.group(0));
    }
}

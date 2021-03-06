package b.nana.technology.gingester.transformers.base.transformers.html;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import org.jsoup.nodes.Element;

@Pure
public final class ToString implements Transformer<Element, String> {

    @Override
    public void transform(Context context, Element in, Receiver<String> out) {
        out.accept(context, in.html());
    }
}

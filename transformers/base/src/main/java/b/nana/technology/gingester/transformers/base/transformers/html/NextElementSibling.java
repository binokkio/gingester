package b.nana.technology.gingester.transformers.base.transformers.html;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import org.jsoup.nodes.Element;

public final class NextElementSibling implements Transformer<Element, Element> {

    @Override
    public void transform(Context context, Element in, Receiver<Element> out) {
        out.accept(context.stash("description", "nextSibling"), in.nextElementSibling());
    }
}

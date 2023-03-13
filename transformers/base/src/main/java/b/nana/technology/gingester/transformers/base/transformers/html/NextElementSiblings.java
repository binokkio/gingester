package b.nana.technology.gingester.transformers.base.transformers.html;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import org.jsoup.nodes.Element;

public final class NextElementSiblings implements Transformer<Element, Element> {

    @Override
    public void transform(Context context, Element in, Receiver<Element> out) {
        Element pointer = in.nextElementSibling();
        while (pointer != null) {
            out.accept(context.stash("description", "sibling"), in.nextElementSibling());
            pointer = pointer.nextElementSibling();
        }
    }
}

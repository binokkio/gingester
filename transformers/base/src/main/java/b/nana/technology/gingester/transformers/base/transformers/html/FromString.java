package b.nana.technology.gingester.transformers.base.transformers.html;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

public final class FromString implements Transformer<String, Element> {

    @Override
    public void transform(Context context, String in, Receiver<Element> out) throws Exception {
        out.accept(context, Jsoup.parse(in));
    }
}
package b.nana.technology.gingester.transformers.base.transformers.html;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public final class ElementsContainingText implements Transformer<Element, Element> {

    private final String text;

    public ElementsContainingText(Parameters parameters) {
        text = parameters.text;
    }

    @Override
    public void transform(Context context, Element in, Receiver<Element> out) throws Exception {
        Elements elements = in.getElementsContainingText(text);
        for (int i = 0; i < elements.size(); i++) {
            out.accept(context.stash("description", text + " :: " + i), elements.get(i));
        }
    }

    public static class Parameters {

        public String text;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String text) {
            this.text = text;
        }
    }
}

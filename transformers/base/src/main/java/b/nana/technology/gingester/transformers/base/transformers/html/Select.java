package b.nana.technology.gingester.transformers.base.transformers.html;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public final class Select implements Transformer<Element, Element> {

    private final String path;

    public Select(Parameters parameters) {
        path = parameters.path;
    }

    @Override
    public void transform(Context context, Element in, Receiver<Element> out) throws Exception {
        Elements elements = in.select(path);
        for (int i = 0; i < elements.size(); i++) {
            out.accept(context.stash("description", path + " :: " + i), elements.get(i));
        }
    }

    public static class Parameters {

        public String path;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String path) {
            this.path = path;
        }
    }
}

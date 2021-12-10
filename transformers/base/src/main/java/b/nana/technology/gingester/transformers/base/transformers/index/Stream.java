package b.nana.technology.gingester.transformers.base.transformers.index;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformers.Fetch;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;
import java.util.NoSuchElementException;

public final class Stream implements Transformer<Object, Object> {

    private final String[] index;

    public Stream(Parameters parameters) {
        index = Fetch.parseStashName(parameters.index);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {

        Map<?, ?> index = (Map<?, ?>) context.fetch(this.index).findFirst()
                .orElseThrow(() -> new NoSuchElementException(String.join("/", this.index)));

        for (Object o : index.keySet()) {
            out.accept(context.stash("key", o), index.get(o));
        }
    }

    public static class Parameters {

        public String index = "index";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String index) {
            this.index = index;
        }
    }
}

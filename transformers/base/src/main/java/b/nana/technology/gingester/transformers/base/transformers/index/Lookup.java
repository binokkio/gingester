package b.nana.technology.gingester.transformers.base.transformers.index;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformers.Fetch;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;
import java.util.NoSuchElementException;

@Names(1)
public final class Lookup implements Transformer<Object, Object> {

    private final String[] index;
    private final boolean throwOnEmptyLookup;

    public Lookup(Parameters parameters) {
        index = Fetch.parseStashName(parameters.index);
        throwOnEmptyLookup = parameters.throwOnEmptyLookup;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {

        Map<?, ?> index = (Map<?, ?>) context.fetch(this.index).findFirst()
                .orElseThrow(() -> new NoSuchElementException(String.join("/", this.index)));

        Object result = index.get(in);

        if (throwOnEmptyLookup && result == null) {
            throw new NoSuchElementException(String.join("/", this.index) + " :: " + in);
        }

        out.accept(context, result);
    }

    public static class Parameters {

        public String index = "index";
        public boolean throwOnEmptyLookup = false;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String index) {
            this.index = index;
        }
    }
}

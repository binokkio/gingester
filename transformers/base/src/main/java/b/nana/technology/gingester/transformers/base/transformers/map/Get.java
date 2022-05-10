package b.nana.technology.gingester.transformers.base.transformers.map;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformers.Fetch;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;
import java.util.NoSuchElementException;

public final class Get implements Transformer<Object, Object> {

    private final String[] map;
    private final boolean throwOnEmptyGet;

    public Get(Parameters parameters) {
        map = Fetch.parseStashName(parameters.map);
        throwOnEmptyGet = parameters.throwOnEmptyGet;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {

        Map<?, ?> map = (Map<?, ?>) context.fetch(this.map).findFirst()
                .orElseThrow(() -> new NoSuchElementException(String.join(".", this.map)));

        Object result = map.get(in);

        if (throwOnEmptyGet && result == null) {
            throw new NoSuchElementException(String.join(".", this.map) + " :: " + in);
        }

        out.accept(context, result);
    }

    public static class Parameters {

        public String map = "";
        public boolean throwOnEmptyGet = false;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String map) {
            this.map = map;
        }
    }
}

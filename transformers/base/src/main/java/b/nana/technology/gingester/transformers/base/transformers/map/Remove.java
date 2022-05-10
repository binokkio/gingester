package b.nana.technology.gingester.transformers.base.transformers.map;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformers.Fetch;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;
import java.util.NoSuchElementException;

public final class Remove implements Transformer<Object, Object> {

    private final String[] map;
    private final boolean throwOnEmptyRemove;

    public Remove(Parameters parameters) {
        map = Fetch.parseStashName(parameters.map);
        throwOnEmptyRemove = parameters.throwOnEmptyRemove;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {

        Map<?, ?> map = (Map<?, ?>) context.fetch(this.map).findFirst()
                .orElseThrow(() -> new NoSuchElementException(String.join(".", this.map)));

        Object result = map.remove(in);

        if (throwOnEmptyRemove && result == null) {
            throw new NoSuchElementException(String.join(".", this.map) + " :: " + in);
        }

        out.accept(context, result);
    }

    public static class Parameters {

        public String map = "";
        public boolean throwOnEmptyRemove = false;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String map) {
            this.map = map;
        }
    }
}

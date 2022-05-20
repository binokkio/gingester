package b.nana.technology.gingester.transformers.base.transformers.map;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;
import java.util.NoSuchElementException;

public final class Get implements Transformer<Object, Object> {

    private final FetchKey fetchMap;
    private final boolean throwOnEmptyGet;

    public Get(Parameters parameters) {
        fetchMap = parameters.map;
        throwOnEmptyGet = parameters.throwOnEmptyGet;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {

        Map<?, ?> map = (Map<?, ?>) context.require(fetchMap);
        Object result = map.get(in);

        if (throwOnEmptyGet && result == null) {
            throw new NoSuchElementException(fetchMap + " :: " + in);
        }

        out.accept(context, result);
    }

    public static class Parameters {

        public FetchKey map = new FetchKey(1);
        public boolean throwOnEmptyGet = false;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(FetchKey map) {
            this.map = map;
        }
    }
}

package b.nana.technology.gingester.transformers.base.transformers.map;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;
import java.util.NoSuchElementException;

public final class Get implements Transformer<Object, Object> {

    private final FetchKey fetchMap;
    private final boolean optional;

    public Get(Parameters parameters) {
        fetchMap = parameters.map;
        optional = parameters.optional;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {

        Map<?, ?> map = (Map<?, ?>) context.require(fetchMap);
        Object result = map.get(in);

        if (result != null) {
            out.accept(context, result);
        } else if (!optional) {
            throw new NoSuchElementException(fetchMap + " :: " + in);
        }
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, map -> o("map", map));
                rule(JsonNode::isArray, array -> o("map", array.get(0), "optional", f("optional", array.get(1))));
            }
        }

        public FetchKey map = new FetchKey(1);
        public boolean optional = false;
    }
}

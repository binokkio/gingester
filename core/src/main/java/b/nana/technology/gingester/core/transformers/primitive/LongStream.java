package b.nana.technology.gingester.core.transformers.primitive;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Names(1)
public final class LongStream implements Transformer<Object, Long> {

    private final long start;
    private final long stop;

    public LongStream(Parameters parameters) {
        start = parameters.startInclusive;
        stop = parameters.endExclusive;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Long> out) {
        for (long i = start; i < stop; i++) {
            out.accept(context, i);
        }
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isInt, stop -> a(0, stop));
                rule(JsonNode::isArray, a -> switch (a.size()) {
                    case 1 -> o("startInclusive", 0, "endExclusive", a.get(0));
                    case 2 -> o("startInclusive", a.get(0), "endExclusive", a.get(1));
                    default -> throw new IllegalArgumentException("Unexpected LongStream parameters: " + a);
                });
            }
        }

        public long startInclusive;
        public long endExclusive;
    }
}

package b.nana.technology.gingester.transformers.base.transformers.primitive;

import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Names(1)
@Example(example = "10", description = "Yield a random integer between 0 (inclusive) and 10 (exclusive)")
@Example(example = "5 10", description = "Yield a random integer between 5 (inclusive) and 10 (exclusive)")
public final class IntRandom implements Transformer<Object, Integer> {

    private final int min;
    private final int max;
    private final Random random;

    public IntRandom(Parameters parameters) {
        min = parameters.min;
        max = parameters.max;
        random = parameters.secure ? new SecureRandom() : new Random();
    }

    @Override
    public void transform(Context context, Object in, Receiver<Integer> out) throws Exception {
        out.accept(context, random.nextInt(min, max));
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isInt, NormalizingDeserializer::a);
                rule(JsonNode::isTextual, NormalizingDeserializer::a);
                rule(JsonNode::isArray, instructions -> {
                    boolean secure = false;
                    List<Integer> bounds = new ArrayList<>();
                    for (JsonNode instruction : instructions) {
                        if (instruction.isTextual()) {
                            if (instruction.textValue().equals("secure!")) {
                                secure = false;
                            } else if (instruction.textValue().equals("!secure")) {
                                secure = true;
                            } else {
                                throw new IllegalArgumentException("Unexpected argument: " + instruction);
                            }
                        } else if (instruction.isInt() && bounds.size() < 2) {
                            bounds.add(instruction.intValue());
                        } else {
                            throw new IllegalArgumentException("Unexpected argument: " + instruction);
                        }
                    }
                    if (bounds.isEmpty()) {
                        return o("secure", secure);
                    } else if (bounds.size() == 1) {
                        return o("max", bounds.get(0), "secure", secure);
                    } else {
                        return o("min", bounds.get(0),"max", bounds.get(1), "secure", secure);
                    }
                });
            }
        }

        public int min;
        public int max = Integer.MAX_VALUE;
        public boolean secure = true;
    }
}

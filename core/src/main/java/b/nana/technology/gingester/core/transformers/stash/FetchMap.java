package b.nana.technology.gingester.core.transformers.stash;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Names(1)
public final class FetchMap implements Transformer<Object, Map<String, Object>> {

    private final List<Instruction> instructions;

    public FetchMap(Parameters parameters) {
        instructions = parameters.instructions;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Map<String, Object>> out) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Instruction instruction : instructions) {
            Optional<Object> value = context.fetch(instruction.fetch);
            if (value.isEmpty()) {
                if (!instruction.optional) {
                    throw new IllegalStateException("No value for " + instruction.fetch);
                }
            }
            else {
                result.put(instruction.as, value.get());
            }
        }
        out.accept(context, result);
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, instruction -> o("instructions", a(instruction)));
                rule(JsonNode::isArray, instructions -> o("instructions", instructions));
            }
        }

        public List<Instruction> instructions;
    }

    public static class Instruction {

        private static final Pattern INSTRUCTION = Pattern.compile("(.+?) ?(?:> ?(.+?))?(\\?)?");

        public FetchKey fetch;
        public String as;
        public boolean optional;

        @JsonCreator
        public Instruction() {}

        @JsonCreator
        public Instruction(String instruction) {

            Matcher matcher = INSTRUCTION.matcher(instruction);
            if (!matcher.matches()) throw new IllegalArgumentException("Invalid FetchMap instruction: " + instruction);

            fetch = new FetchKey(matcher.group(1));
            as = matcher.group(2) != null ? matcher.group(2) : fetch.toString();
            optional = matcher.group(3) != null;
        }
    }
}

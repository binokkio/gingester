package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Names(1)
public final class Merge implements Transformer<Object, Object> {

    private final ContextMap<Map<String, List<Object>>> state = new ContextMap<>();
    private final List<Instruction> instructions;

    public Merge(Parameters parameters) {
        instructions = parameters.instructions;
        for (Instruction instruction : instructions) {
            requireNonNull(instruction.fetch);
            if (instruction.fetch.isOrdinal()) {
                requireNonNull(instruction.stash);
            } else if (instruction.stash == null) {
                instruction.stash = instruction.fetch.toString();
            }
        }
    }

    @Override
    public void prepare(Context context, Receiver<Object> out) throws Exception {
        Map<String, List<Object>> merged = new HashMap<>();
        for (Instruction instruction : instructions) {
            merged.put(instruction.stash, new ArrayList<>());
        }
        state.put(context, merged);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        state.act(context, merged -> {
            for (Instruction instruction : instructions) {
                context.fetch(instruction.fetch).ifPresent(
                        object -> merged.get(instruction.stash).add(object));
            }
        });
    }

    @Override
    public void finish(Context context, Receiver<Object> out) throws Exception {

        Map<String, List<Object>> merged = state.remove(context);
        Map<String, Object> stash = new HashMap<>(merged.size());

        for (Instruction instruction : instructions) {
            List<Object> values = merged.get(instruction.stash);
            if (instruction.list) {
                stash.put(instruction.stash, values);
            } else {
                if (values.size() == 1) {
                    stash.put(instruction.stash, values.get(0));
                } else if (!values.isEmpty()){
                    throw new IllegalStateException("Multiple values for " + instruction.stash);
                }
            }
        }

        out.accept(context.stash(stash), "merge signal");
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {

        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(j -> !j.isArray(), instruction -> o("instructions", a(instruction)));
                rule(JsonNode::isArray, instructions -> o("instructions", instructions));
            }
        }

        public List<Instruction> instructions;
    }

    @JsonDeserialize(using = Instruction.Deserializer.class)
    public static class Instruction {

        public static class Deserializer extends NormalizingDeserializer<Instruction> {
            public Deserializer() {
                super(Instruction.class);
                rule(JsonNode::isTextual, fetch -> o("fetch", fetch));
            }
        }

        public FetchKey fetch;
        public String stash;
        public boolean list;
    }
}

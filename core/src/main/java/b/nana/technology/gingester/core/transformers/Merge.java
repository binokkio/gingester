package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

@Names(1)
@Example(example = "hello world", description = "Collect `hello` and `world` stashes and stash them on finish")
@Example(example = "hello world?", description = "Same as first example but `world` is optional")
@Example(example = "hello 'world > worlds[]'", description = "Same as first example but stashing a list of `world` as `worlds`")
@Example(example = "hello 'world > worlds[]?'", description = "Same as above but without an exception if `worlds` ends up empty")
@Example(example = "'hello > world'", description = "Collect `hello` and stash it as `world` on finish")
public final class Merge implements Transformer<Object, Object> {

    private final ContextMap<State> states = new ContextMap<>();
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
        State state = new State();
        for (Instruction instruction : instructions) {
            if (instruction.list) {
                state.lists.put(instruction.stash, new ArrayList<>());
            }
        }
        states.put(context, state);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        states.act(context, state -> {
            for (Instruction instruction : instructions) {
                if (instruction.list) {
                    context.fetch(instruction.fetch).ifPresent(
                            object -> state.lists.get(instruction.stash).add(object));
                } else {
                    context.fetch(instruction.fetch).ifPresent(
                            object -> {
                                Object collision = state.singles.put(instruction.stash, object);
                                if (collision != null && collision != object) {
                                    throw new IllegalStateException("Multiple values for " + instruction.stash);
                                }
                            });
                }
            }
        });
    }

    @Override
    public void finish(Context context, Receiver<Object> out) throws Exception {

        State state = states.remove(context);
        Map<String, Object> stash = new HashMap<>(instructions.size());

        for (Instruction instruction : instructions) {
            if (instruction.list) {
                List<Object> values = state.lists.get(instruction.stash);
                if (!instruction.optional && values.isEmpty()) {
                    throw new IllegalStateException("No values for \"" + instruction + "\"");
                }
                stash.put(instruction.stash, state.lists.get(instruction.stash));
            } else {
                Object value = state.singles.get(instruction.stash);
                if (!instruction.optional && value == null) {
                    throw new IllegalStateException("No value for \"" + instruction + "\"");
                }
                stash.put(instruction.stash, value);
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

    public static class Instruction {

        // TODO could put initialCapacity inside the []
        private static final Pattern INSTRUCTION = Pattern.compile("(.+?) ?(?:> ?(.+?)(\\[])?)?(\\?)?");

        public FetchKey fetch;
        public String stash;
        public boolean list;
        public boolean optional;

        @JsonCreator
        public Instruction() {}

        @JsonCreator
        public Instruction(String instruction) {

            Matcher matcher = INSTRUCTION.matcher(instruction);
            if (!matcher.matches()) throw new IllegalArgumentException("Invalid merge instruction: " + instruction);

            fetch = new FetchKey(matcher.group(1));
            if (matcher.group(2) != null) stash = matcher.group(2);
            list = matcher.group(3) != null;
            optional = matcher.group(4) != null;
        }

        @JsonValue
        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder(fetch.toString());
            if (optional) stringBuilder.append('?');
            if (stash != null) stringBuilder.append(" > ").append(stash);
            if (list) stringBuilder.append("[]");
            return stringBuilder.toString();
        }
    }

    private static class State {
        final Map<String, Object> singles = new HashMap<>();
        final Map<String, List<Object>> lists = new HashMap<>();
    }
}

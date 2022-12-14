package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Description;
import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.StashDetails;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

@Names(1)
@Description("Collect stashed values from all in-sync transforms and stash them on finish")
@Example(example = "hello world", description = "Collect `hello` and `world` stashes")
@Example(example = "hello world?", description = "Same as example 1 but `world` is optional")
@Example(example = "hello 'world > worlds[]'", description = "Same as example 1 but place all `world` stashes in a list `worlds`")
@Example(example = "hello 'world > worlds[]?'", description = "Same as above but without an exception if `worlds` ends up empty")
@Example(example = "'hello > world'", description = "Collect `hello` and stash it as `world` on finish")
@Example(example = "'^1 > stashes{tree}'", description = "Collect most recently stashed items in a TreeSet stashed as `stashes`")
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
    public StashDetails getStashDetails() {
        Map<String, Object> types = new HashMap<>();
        for (Instruction instruction : instructions) {
            if (instruction.collect != null) {
                types.put(instruction.stash, instruction.collect.getCollectionType());
            } else {
                types.put(instruction.stash, instruction.fetch);
            }
        }
        return StashDetails.of(types);
    }

    @Override
    public void prepare(Context context, Receiver<Object> out) {
        State state = new State();
        for (Instruction instruction : instructions) {
            if (instruction.collect != null) {
                state.collections.put(instruction.stash, instruction.collect.collectionSupplier.get());
            }
        }
        states.put(context, state);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        states.act(context, state -> {
            for (Instruction instruction : instructions) {
                if (instruction.collect != null) {
                    context.fetch(instruction.fetch).ifPresent(
                            object -> state.collections.get(instruction.stash).add(object));
                } else {
                    context.fetch(instruction.fetch).ifPresent(
                            object -> {
                                Object collision = state.singles.put(instruction.stash, object);
                                if (collision != null && !Objects.equals(collision, object)) {
                                    throw new IllegalStateException("Multiple values for " + instruction.stash);
                                }
                            });
                }
            }
        });
    }

    @Override
    public void finish(Context context, Receiver<Object> out) {

        State state = states.remove(context);
        Map<String, Object> stash = new HashMap<>(instructions.size());

        for (Instruction instruction : instructions) {
            if (instruction.collect != null) {
                Collection<Object> values = state.collections.get(instruction.stash);
                if (!instruction.optional && values.isEmpty()) {
                    throw new IllegalStateException("No values for \"" + instruction + "\"");
                }
                stash.put(instruction.stash, state.collections.get(instruction.stash));
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

    private static class State {
        final Map<String, Object> singles = new HashMap<>();
        final Map<String, Collection<Object>> collections = new HashMap<>();
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

        private static final Pattern INSTRUCTION = Pattern.compile("([a-zA-Z^0-9.]+?) ?(?:> ?([a-zA-Z0-9]+?)(\\[[a-z]*?]|\\{[a-z]*?})?)?(\\?)?");

        public FetchKey fetch;
        public String stash;
        public CollectInstruction collect;
        public boolean optional;

        @JsonCreator
        public Instruction() {}

        @JsonCreator
        public Instruction(String instruction) {

            Matcher matcher = INSTRUCTION.matcher(instruction);
            if (!matcher.matches()) throw new IllegalArgumentException("Invalid merge instruction: " + instruction);

            fetch = new FetchKey(matcher.group(1));
            if (matcher.group(2) != null) stash = matcher.group(2);
            collect = matcher.group(3) != null ? new CollectInstruction(matcher.group(3)) : null;
            optional = matcher.group(4) != null;
        }

        @JsonValue
        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder(fetch.toString());
            if (optional) stringBuilder.append('?');
            if (stash != null) stringBuilder.append(" > ").append(stash);
            if (collect != null) stringBuilder.append(collect);
            return stringBuilder.toString();
        }
    }

    public static class CollectInstruction {

        private final String description;
        private final Supplier<Collection<Object>> collectionSupplier;

        public CollectInstruction(String description) {
            this.description = description;
            this.collectionSupplier = getCollectionSupplier(description);
        }

        private Class<?> getCollectionType() {
            return description.startsWith("[") ? List.class : Set.class;
        }

        private Supplier<Collection<Object>> getCollectionSupplier(String description) {
            switch (description) {
                case "[]":
                case "[array]": return ArrayList::new;
                case "[linked]": return LinkedList::new;
                case "{}":
                case "{hash}": return HashSet::new;
                case "{Linked}": return LinkedHashSet::new;
                case "{tree}": return TreeSet::new;
                default: throw new IllegalArgumentException("Invalid collect instruction: " + description);
            }
        }

        @JsonValue
        @Override
        public String toString() {
            return description;
        }
    }
}

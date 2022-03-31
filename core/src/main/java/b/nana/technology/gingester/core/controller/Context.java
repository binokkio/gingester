package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Context implements Iterable<Context> {

    private static final int INDENT = 2;

    /**
     * Create a new Template.
     *
     * See {@link Template} for details.
     *
     * @param templateParameters the template parameters
     * @return the template
     */
    public static Template newTemplate(TemplateParameters templateParameters) {
        return new Template(templateParameters);
    }

    /**
     * Create a new TemplateMapper.
     *
     * See {@link TemplateMapper} for details.
     *
     * @param templateParameters the template parameters
     * @return the template mapper
     */
    public static <T> TemplateMapper<T> newTemplateMapper(TemplateParameters templateParameters, Function<String, T> mapper) {
        return new TemplateMapper<>(templateParameters, mapper);
    }

    public static Context newSeedContext(Controller<?, ?> seedController) {
        return new Context(null, null, true, seedController, Map.of());
    }

    public static Context newTestContext() {
        return new Context(null, null, true, new Controller<>("__test_seed__"), Map.of());
    }


    private final Context parent;
    private final Context group;
    private final boolean synced;
    final Controller<?, ?> controller;
    private final Map<String, Object> stash;
    private volatile boolean isFlawless = true;

    private Context(Context parent, Context group, boolean synced, Controller<?, ?> controller, Map<String, Object> stash) {
        this.parent = parent;
        this.group = group;
        this.synced = synced;
        this.controller = controller;
        this.stash = stash;
    }

    private Context(Builder builder) {
        this(builder.parent, builder.group, builder.synced, builder.controller, builder.stash);
    }

    public boolean isSeed() {
        return parent == null;
    }

    public boolean hasGroup() {
        return group != null;
    }

    public boolean isSynced() {
        return synced;
    }

    public boolean isFlawless() {
        return isFlawless;
    }

    void markFlawed() {
        isFlawless = false;
    }

    /**
     * Fetch object(s) from stash.
     * <p>
     * Iterates through the transformers that contributed to this context, from last to first. Checks the stash each
     * transformer provided for an object that matches the given name.
     * <p>
     * The name is used to step through the layers of the stash. If a transformer stashed the following:
     * <pre>
     * context.stash(Map.of(
     *     "foo", Map.of(
     *         "bar": 123
     *    )
     * ))
     * </pre>
     * <p>
     * Then {@code context.fetch("foo", "bar").findFirst().orElseThrow()} would return 123.
     * <p>
     * Fetch will step through instances of Map, List, and JsonNode.
     * <p>
     * To steer fetch towards a specific transformer, the transformer id should be given as the first name.
     *
     * @param name the stash name, e.g. {@code fetch("Path.Search", "description")};
     * @return stream of stashes matching the given name
     */
    public Stream<Object> fetch(String... name) {
        return Stream.concat(
                        stream().map(c -> c.stash).filter(Objects::nonNull),
                        stream().filter(c -> c.stash != null).map(c -> Map.of(c.controller.id, c.stash))
                ).map(s -> {
                    Object result = s;
                    for (String n : name) {
                        if (result instanceof Map) {
                            result = ((Map<?, ?>) result).get(n);
                        } else if (result instanceof List) {
                            result = ((List<?>) result).get(Integer.parseInt(n));
                        } else if (result instanceof JsonNode) {
                            JsonNode jsonNode = (JsonNode) result;
                            if (jsonNode.isObject()) {
                                result = jsonNode.get(n);
                            } else if (jsonNode.isArray()) {
                                result = jsonNode.get(Integer.parseInt(n));
                            } else {
                                result = null;
                            }
                        } else {
                            return null;
                        }
                    }
                    return result;
                })
                .filter(Objects::nonNull);
    }

    /**
     * Fetch object(s) from stash, reversed.
     *
     * @param name the stash name, e.g. {@code fetch("Path.Search", "description")};
     * @return the same as {@link #fetch(String...)}, but reversed.
     */
    public Stream<Object> fetchReverse(String... name) {
        List<Object> results = fetch(name).collect(Collectors.toCollection(ArrayList::new));
        Collections.reverse(results);
        return results.stream();
    }

    @Override
    public Iterator<Context> iterator() {
        return new Iterator<>() {

            private Context pointer = Context.this;
            private boolean nextIsGroup = false;

            @Override
            public boolean hasNext() {
                return pointer != null;
            }

            @Override
            public Context next() {
                Context next;
                if (nextIsGroup) {
                    next = pointer.group;
                    pointer = pointer.parent;
                    nextIsGroup = false;
                } else {
                    next = pointer;
                    if (next.hasGroup()) nextIsGroup = true;
                    else pointer = pointer.parent;
                }
                return next;
            }
        };
    }

    public Stream<Context> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    public String prettyStash() {
        List<Context> contexts = stream().collect(Collectors.toList());
        Collections.reverse(contexts);
        Map<Object, Object> combined = new LinkedHashMap<>();
        for (Context context : contexts) {
            combined.put(
                    new StringBuilder(context.controller.id),  // TODO quick fix to prevent key collisions
                    context.stash != null ? context.stash : "{}"
            );
        }
        return pretty(combined, 0, false);
    }

    private String pretty(Object object, int indentation, boolean sort) {

        if (object instanceof Map) {

            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append("{\n");

            Stream<? extends Map.Entry<?, ?>> stream = ((Map<?, ?>) object).entrySet().stream();
            if (sort) stream = stream.sorted(Comparator.comparing(entry -> entry.getKey().toString()));

            stream.forEach(e -> stringBuilder
                    .append(" ".repeat(indentation + INDENT))
                    .append(e.getKey())
                    .append(": ")
                    .append(pretty(e.getValue(), indentation + INDENT, true)));

            stringBuilder
                    .append(" ".repeat(indentation))
                    .append("}\n");

            return stringBuilder.toString();

        } else {
            return object + "\n";
        }
    }

    /**
     * Create a new builder with this context as parent.
     *
     * @return the context builder
     */
    public Builder extend() {
        return new Builder(this);
    }

    /**
     * Create a new builder with this context as parent and the given key-value as stash.
     *
     * @param key the key to stash
     * @param value the value to stash
     * @return the context builder
     */
    public Builder stash(String key, Object value) {
        return new Builder(this).stash(key, value);
    }

    /**
     * Create a new builder with this context as parent and the given stash.
     *
     * @param stash the stash
     * @return the context builder
     */
    public Builder stash(Map<String, Object> stash) {
        return new Builder(this).stash(stash);
    }

    public static final class Builder {

        private final Context parent;
        private Context group;
        private boolean synced;
        private Controller<?, ?> controller;
        private Map<String, Object> stash;

        private Builder(Context parent) {
            this.parent = parent;
        }

        public Builder group(Context group) {
            this.group = group;
            return this;
        }

        boolean hasGroup() {
            return group != null;
        }

        Builder synced(boolean synced) {
            this.synced = synced;
            return this;
        }

        /**
         * Replace the current stash, if any, with the given key-value.
         *
         * @param key the key to stash
         * @param value the value to stash
         * @return this builder
         */
        public Builder stash(String key, Object value) {
            this.stash = Map.of(key, value);
            return this;
        }

        /**
         * Replace the current stash, if any, with the given stash.
         *
         * @param stash the stash
         * @return this builder
         */
        public Builder stash(Map<String, Object> stash) {
            this.stash = stash;
            return this;
        }

        /**
         * Build context with a dummy controller, only for testing!
         *
         * @return the context
         */
        public Context buildForTesting() {
            this.controller = new Controller<>("__test__");
            return new Context(this);
        }

        Context build(Controller<?, ?> controller) {
            this.controller = controller;
            return new Context(this);
        }
    }
}

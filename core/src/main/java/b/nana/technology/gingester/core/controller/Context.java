package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.Id;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Context implements Iterable<Context> {

    private static final int INDENT = 2;

    /**
     * Create a new Template.
     * <p>
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
     * <p>
     * See {@link TemplateMapper} for details.
     *
     * @param templateParameters the template parameters
     * @return the template mapper
     */
    public static <T> TemplateMapper<T> newTemplateMapper(TemplateParameters templateParameters, TemplateMapper.Mapper<T> mapper) {
        return new TemplateMapper<>(templateParameters, mapper);
    }

    public static Context newSeedContext(Controller<?, ?> seedController) {
        return new Context(null, null, true, seedController, null);
    }

    public static Context newTestContext() {
        return new Context(null, null, true, new Controller<>(Id.newTestId("$__test_seed__")), null);
    }


    private final Context parent;
    private final Context group;
    private final boolean synced;
    final Controller<?, ?> controller;
    private final Map<String, Object> stash;
    private volatile boolean isFlawed;

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

    public String getTransformerId() {
        return controller.id.toString();
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

    /**
     * Check if this context is flawless.
     * <p>
     * When a transformer throws an exception in a `prepare`, `transform`, or `finish` call the context of that
     * call is marked as flawed. The exception then traverses the context chain up to the first context from a
     * transformer that has an exception handler, inclusive, marking all those contexts as flawed. The exception
     * is then passed to the exception handler(s) starting a new flawless context.
     *
     * @return true if this context was not marked flawed, false if it was
     */
    public boolean isFlawless() {
        return !isFlawed;
    }

    void markFlawed() {
        isFlawed = true;
    }


    /**
     * Convenience method for {@link #fetchAll(FetchKey)}.{@link Stream#findFirst()}.
     */
    public Optional<Object> fetch(FetchKey fetchKey) {
        return fetchAll(fetchKey).findFirst();
    }

    /**
     * Convenience method for {@link #fetchAll(FetchKey)}.{@link Stream#findFirst()}.{@link Optional#orElseThrow()}}.
     */
    public Object require(FetchKey fetchKey) {
        return fetchAll(fetchKey).findFirst().orElseThrow(() -> new NoSuchElementException("Empty fetch for " + fetchKey));
    }

    /**
     * Fetch objects from stash.
     * <p>
     * Iterates through the transformers that contributed to this context, from last to first. Checks the stash each
     * transformer provided for an object that matches the given fetch key.
     * <p>
     * The fetch key is used to step through the layers of the stash. If a transformer stashed the following:
     * <pre>
     * context.stash(Map.of(
     *     "foo", Map.of(
     *         "bar": 123
     *    )
     * ))
     * </pre>
     * <p>
     * Then {@code context.fetchAll(new FetchKey("foo.bar"))} will return a stream containing only 123.
     * <p>
     * Fetch will step through instances of Map, List, and JsonNode.
     * <p>
     * To steer fetch towards a specific transformer, the transformer id must be given as the first fetch key
     * part, e.g. new FetchKey("Transformer.foo.bar").
     * <p>
     * Alternatively an ordinal fetch can be performed, using e.g. new FetchKey("^2"), which will fetch not the
     * most recently stashed value but the one before that.
     *
     * @param fetchKey descriptor of the objects to fetch
     * @return stream of objects that are stashed at the given fetch key
     */
    public Stream<Object> fetchAll(FetchKey fetchKey) {
        if (fetchKey.isOrdinal()) {
            int ordinal = 1;
            Controller<?, ?> last = null;
            for (Context context : this) {
                Optional<FetchKey> ordinalFetchKey = context.controller.stashDetails.getOrdinal();
                if (ordinalFetchKey.isPresent()) {
                    if (ordinal == fetchKey.ordinal()) {
                        return context.fetchAll(ordinalFetchKey.get()).limit(1);
                    } else if (context.controller != last) {  // prevent counting grouped contexts twice
                        ordinal++;
                        last = context.controller;
                    }
                }
            }
            return Stream.empty();
        } else {
            return (fetchKey.hasTarget() ? stream().filter(c -> fetchKey.matchesTarget(c.controller.id)) : stream())
                    .map(c -> c.stash)
                    .filter(Objects::nonNull)
                    .map(s -> {
                        Object result = s;
                        for (String n : fetchKey.getNames()) {
                            if (result instanceof Map) {
                                result = ((Map<?, ?>) result).get(n);
                            } else if (result instanceof List) {
                                result = ((List<?>) result).get(Integer.parseInt(n));  // TODO surround with try-catch-ignore?
                            } else if (result instanceof JsonNode) {
                                JsonNode jsonNode = (JsonNode) result;
                                if (jsonNode.isObject()) {
                                    result = jsonNode.get(n);
                                } else if (jsonNode.isArray()) {
                                    result = jsonNode.get(Integer.parseInt(n));  // TODO surround with try-catch-ignore?
                                } else {
                                    return null;
                                }
                            } else {
                                return null;
                            }
                        }
                        return result;
                    })
                    .filter(Objects::nonNull);
        }
    }

    /**
     * Fetch objects from stash, reversed.
     * <p>
     * See {@link #fetchAll(FetchKey)} for details.
     *
     * @param fetchKey descriptor of the objects to fetch
     * @return the same as {@link #fetchAll(FetchKey)}, but reversed.
     */
    public Stream<Object> fetchReverse(FetchKey fetchKey) {
        List<Object> results = fetchAll(fetchKey).collect(Collectors.toCollection(ArrayList::new));
        Collections.reverse(results);
        return results.stream();
    }

    /**
     * Convenient but slower version of {@link #fetch(FetchKey)}.
     */
    public Optional<Object> fetch(String... fetchKey) {
        return fetch(new FetchKey(String.join(".", fetchKey)));
    }

    /**
     * Convenient but slower version of {@link #require(FetchKey)}.
     */
    public Object require(String... fetchKey) {
        return require(new FetchKey(String.join(".", fetchKey)));
    }

    /**
     * Convenient but slower version of {@link #fetchAll(FetchKey)}.
     */
    public Stream<Object> fetchAll(String... fetchKey) {
        return fetchAll(new FetchKey(String.join(".", fetchKey)));
    }

    /**
     * Convenient but slower version of {@link #fetchReverse(FetchKey)}.
     */
    public Stream<Object> fetchReverse(String... fetchKey) {
        return fetchReverse(new FetchKey(String.join(".", fetchKey)));
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

    public Stream<Context> streamReverse() {
        List<Context> contexts = stream().collect(Collectors.toList());
        Collections.reverse(contexts);
        return contexts.stream();
    }

    public String prettyStash(int limit) {
        List<Context> contexts = stream().collect(Collectors.toList());
        Collections.reverse(contexts);
        Map<Object, Object> combined = new LinkedHashMap<>();
        for (Context context : contexts) {
            if (context.stash != null && !context.stash.isEmpty()) {
                combined.put(
                        new PrettyStashKey(context.controller.id.toString()),  // can't use the id as-is since grouping can cause it to occur multiple times
                        context.stash
                );
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        pretty(combined, limit, 0, true, stringBuilder);
        return stringBuilder.toString();
    }

    private void pretty(Object object, int limit, int indentation, boolean root, StringBuilder stringBuilder) {

        if (object instanceof Map) {

            stringBuilder.append("{\n");

            int size = ((Map<?, ?>) object).size();
            Stream<? extends Map.Entry<?, ?>> stream = ((Map<?, ?>) object).entrySet().stream();
            if (!root)
                stream = stream
                        .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                        .limit(size > limit + 1 ? limit : size);

            stream.forEach(e -> {
                stringBuilder
                        .append(" ".repeat(indentation + INDENT))
                        .append(e.getKey())
                        .append(": ");
                pretty(e.getValue(), limit, indentation + INDENT, false, stringBuilder);
            });

            if (!root && size > limit + 1)
                stringBuilder
                        .append(" ".repeat(indentation + INDENT))
                        .append("...and ")
                        .append(size - limit)
                        .append(" more\n");

            stringBuilder
                    .append(" ".repeat(indentation))
                    .append("}\n");

        } else if (object instanceof List) {
            int size = ((List<?>) object).size();
            Stream<?> stream = ((List<?>) object).stream();
            prettyEntries(indentation, '[', ']', size, limit, stream, stringBuilder);
        } else if (object instanceof Set) {
            int size = ((Set<?>) object).size();
            Stream<?> stream = ((Set<?>) object).stream();
            prettyEntries(indentation, '{', '}', size, limit, stream, stringBuilder);
        } else {

            String string = Objects.toString(object);
            String truncated = string.substring(0, Math.min(string.length(), limit * 100));
            String[] lines = truncated.split("\\r?\\n");

            if (lines.length == 1 && lines[0].length() == string.length()) {
                stringBuilder.append(lines[0]).append('\n');
            } else {

                for (int i = 0; i < lines.length && i < limit; i++)
                    stringBuilder
                            .append('\n')
                            .append(" ".repeat(indentation + INDENT))
                            .append(lines[i]);

                if (truncated.length() < string.length() || limit < lines.length)
                    stringBuilder
                            .append('\n')
                            .append(" ".repeat(indentation + INDENT))
                            .append("...and more, total length is ")
                            .append(string.length());

                stringBuilder.append('\n');
            }
        }
    }

    private void prettyEntries(int indentation, char begin, char end, int size, int limit, Stream<?> stream, StringBuilder stringBuilder) {

        stringBuilder.append(begin).append('\n');

        stream.limit(size > limit + 1 ? limit : size).forEach(o -> {
            stringBuilder.append(" ".repeat(indentation + INDENT));
            pretty(o, limit, indentation + INDENT, false, stringBuilder);
        });

        if (size > limit + 1)
            stringBuilder
                    .append(" ".repeat(indentation + INDENT))
                    .append("...and ")
                    .append(size - limit)
                    .append(" more\n");

        stringBuilder
                .append(" ".repeat(indentation))
                .append(end)
                .append('\n');
    }

    @Override
    public String toString() {
        return '[' + controller.id.toString() + " context]";
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
     * Create a new builder with this context as parent and the given group as group.
     *
     * @param group the group
     * @return the context builder
     */
    public Builder group(Context group) {
        return new Builder(this).group(group);
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

        /**
         * Replace the group this context is to be part of when built.
         *
         * @param group the group to add the to-be-build context to
         * @return this builder
         */
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
         * Get the current stash, if any.
         *
         * @return the current stash, is any
         */
        public Optional<Map<String, Object>> getStash() {
            return Optional.ofNullable(stash);
        }

        /**
         * Replace the current stash, if any, with the given key-value.
         *
         * @param key the key to stash
         * @param value the value to stash
         * @return this builder
         */
        public Builder stash(String key, Object value) {
            this.stash = Collections.singletonMap(key, value);
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
            this.controller = new Controller<>(Id.newTestId("$__test__"));
            return new Context(this);
        }

        /**
         * Build context with a dummy controller, must not be passed to a {@link b.nana.technology.gingester.core.receiver.Receiver}!
         *
         * @return the context
         */
        public Context buildForSelf() {
            this.controller = new Controller<>(Id.newTestId("$__self__"));
            return new Context(this);
        }

        Context build(Controller<?, ?> controller) {
            this.controller = controller;
            return new Context(this);
        }
    }

    private static class PrettyStashKey {

        private final String value;

        private PrettyStashKey(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}

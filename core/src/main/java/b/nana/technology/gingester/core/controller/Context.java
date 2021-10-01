package b.nana.technology.gingester.core.controller;

import b.nana.technology.gingester.core.freemarker.FreemarkerContextWrapper;
import b.nana.technology.gingester.core.freemarker.FreemarkerTemplateFactory;
import b.nana.technology.gingester.core.freemarker.FreemarkerTemplateWrapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Context implements Iterable<Context> {

    private static final int INDENT = 2;

    /**
     * Create a new Context.Template.
     *
     * See {@link Template} for details.
     *
     * @param template the template string
     * @return the compiled template
     */
    public static Template newTemplate(String template) {
        FreemarkerTemplateWrapper wrapper = FreemarkerTemplateFactory.createTemplate(template, FreemarkerContextWrapper::new);
        return wrapper::render;
    }


    private final Context parent;
    final Controller<?, ?> controller;
    private final Map<String, Object> stash;
    private final boolean exception;

    private Context(Builder builder) {
        parent = builder.parent;
        controller = builder.controller;
        stash = builder.stash;
        exception = builder.exception;
    }

    public boolean isSeed() {
        return parent == null;
    }

    public boolean isException() {
        return exception;
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
                        } else if (result instanceof JsonNode) {
                            JsonNode jsonNode = (JsonNode) result;
                            if (jsonNode.isObject()) {
                                result = jsonNode.get(n);
                            } else if (jsonNode.isArray()) {
                                ((JsonNode) result).get(Integer.parseInt(n));
                            } else {
                                result = null;
                            }
                        } else if (result instanceof List) {
                            result = ((List<?>) result).get(Integer.parseInt(n));
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

            @Override
            public boolean hasNext() {
                return pointer != null;
            }

            @Override
            public Context next() {
                Context self = pointer;
                pointer = pointer.parent;
                return self;
            }
        };
    }

    public Stream<Context> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    public String prettyStash() {
        List<Context> contexts = stream().collect(Collectors.toList());
        Collections.reverse(contexts);
        Map<String, Object> combined = new LinkedHashMap<>();
        for (Context context : contexts) {
            combined.put(
                    context.controller.id,
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
        private Controller<?, ?> controller;
        private Map<String, Object> stash;
        private boolean exception;

        public Builder() {
            parent = null;
        }

        public Builder(Context parent) {
            this.parent = parent;
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

        Builder exception() {
            exception = true;
            return this;
        }

        /**
         * Build context without controller, only for seeding/testing!
         *
         * @return the context
         */
        public Context build() {
            this.controller = new Controller<>();
            return new Context(this);
        }

        public Context build(Controller<?, ?> controller) {
            this.controller = controller;
            return new Context(this);
        }
    }

    /**
     * Context template.
     * <p>
     * Render strings using the Apache FreeMarker template engine and the Gingester Context as its data model. Template
     * variables are resolved as if they were interpreted by {@link Context#fetch(String...)}.
     */
    public interface Template {
        String render(Context context);
    }
}

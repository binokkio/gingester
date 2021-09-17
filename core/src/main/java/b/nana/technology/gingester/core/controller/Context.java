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

    public static Template newTemplate(String template) {
        FreemarkerTemplateWrapper wrapper = FreemarkerTemplateFactory.createTemplate(template, FreemarkerContextWrapper::new);
        return wrapper::render;
    }


    private final Context parent;
    final Controller<?, ?> controller;  // TODO reduce visibility
    private final Map<String, Object> stash;

    private Context(Builder builder) {
        parent = builder.parent;
        controller = builder.controller;
        stash = builder.stash;
    }

    public boolean isSeed() {
        return parent == null;
    }

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

    public Builder extend() {
        return new Builder(this);
    }

    public Builder stash(String key, Object value) {
        return new Builder(this).stash(Map.of(key, value));
    }

    public Builder stash(Map<String, Object> stash) {
        return new Builder(this).stash(stash);
    }

    public static final class Builder {

        private final Context parent;
        private Controller<?, ?> controller;
        private Map<String, Object> stash;

        public Builder() {
            parent = null;
        }

        public Builder(Context parent) {
            this.parent = parent;
        }

        public Builder stash(String key, Object value) {
            this.stash = Map.of(key, value);
            return this;
        }

        public Builder stash(Map<String, Object> stash) {
            this.stash = stash;
            return this;
        }

        public Context build() {
            this.controller = new Controller<>();
            return new Context(this);
        }

        public Context build(Controller<?, ?> controller) {
            this.controller = controller;
            return new Context(this);
        }
    }

    public interface Template {
        String render(Context context);
    }
}

package b.nana.technology.gingester.core.context;

import b.nana.technology.gingester.core.controller.Controller;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Context implements Iterable<Context> {

    private static final int INDENT = 2;

    public static Context newSeed() {
        return newSeed(null);
    }

    public static Context newSeed(Controller<?, ?> seedController) {
        return new Context(seedController);
    }


    private final Context parent;
    public final Controller<?, ?> controller;
    private final Map<String, Object> stash;

    private Context(Controller<?, ?> seedController) {
        parent = null;
        controller = seedController;
        stash = Collections.emptyMap();
    }

    private Context(Builder builder) {
        parent = builder.parent;
        controller = builder.controller;
        stash = builder.stash;
    }

    public boolean isSeed() {
        return parent == null;
    }

    public Stream<Object> fetch(String... name) {
        return stream()
                .map(c -> c.stash)
                .filter(Objects::nonNull)
                .map(s -> {
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
                            result = null;
                        }
                    }
                    return result;
                })
                .filter(Objects::nonNull);
    }

    @Override
    public Iterator<Context> iterator() {
        return new Iterator<>() {

            private Context pointer = Context.this;

            @Override
            public boolean hasNext() {
                return !pointer.isSeed();
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
                    context.stash
            );
        }
        if (combined.isEmpty()) return "";
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
                    .append('=')
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

    public static class Builder {

        private final Context parent;
        private Controller<?, ?> controller;
        private Map<String, Object> stash;

        private Builder(Context parent) {
            this.parent = parent;
        }

        public Builder controller(Controller<?, ?> controller) {
            this.controller = controller;
            return this;
        }

        public Builder stash(Map<String, Object> stash) {
            this.stash = stash;
            return this;
        }

        public Context build() {
            return new Context(this);
        }
    }
}

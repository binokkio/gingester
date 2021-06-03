package b.nana.technology.gingester.core;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Context implements Iterable<Context> {

    private static final int INDENT = 2;
    static final Context SEED = new Context();

    final Context parent;
    final int depth;
    final Transformer<?, ?> transformer;
    final String description;
    private final Map<String, Object> stash;

    /*
     * TODO
     *
     * These exception listeners are now a bit awkward, information moving upstream.
     * 
     */

    private final BiConsumer<Context, Throwable> exceptionListener;
    private final BiConsumer<Context, Throwable> syncedExceptionListener;

    private Context() {
        parent = null;
        depth = 0;
        transformer = null;
        description = null;
        stash = Collections.emptyMap();
        exceptionListener = null;
        syncedExceptionListener = null;
    }

    private Context(Builder builder) {
        parent = builder.parent != SEED ? builder.parent : null;
        depth = builder.parent != SEED ? builder.parent.depth + 1 : 0;
        transformer = builder.transformer;
        description = builder.description;
        stash = builder.stash != null ? builder.stash : Collections.emptyMap();
        exceptionListener = builder.exceptionListener;
        syncedExceptionListener = builder.syncedExceptionListener;
    }

    public Context.Builder extend(Transformer<?, ?> transformer) {
        return new Context.Builder(this, transformer);
    }

    public void signal() {
        // TODO
    }

    private String[] getDescriptions() {
        Context pointer = this;
        String[] descriptions = new String[depth + 1];
        for (int i = depth; i >= 0; i--) {
            descriptions[i] = pointer.description;
            pointer = pointer.parent;
        }
        return descriptions;
    }

    public String getDescription() {
        return Arrays.stream(getDescriptions())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" :: "));
    }

    public Optional<String> getDescriptionTail() {
        Context pointer = this;
        do {
            if (pointer.description != null) {
                return Optional.of(pointer.description);
            }
            pointer = pointer.parent;
        } while (pointer != null);
        return Optional.empty();
    }

    public Optional<Object> fetch(String... name) {
        if (name.length == 0) {
            throw new IllegalArgumentException("Name must be at least 1 part long");
        } else if (name.length == 1 && name[0].equals("description")) {
            return Optional.of(getDescription());
        } else {
            for (Context context : this) {
                Object object = context.stash.get(name[0]);
                if (object != null) {
                    return resolveDetail(object, name);
                }
            }
            return Optional.empty();
        }
    }

    @Override
    public Iterator<Context> iterator() {
        return new Iterator<>() {

            private Context pointer = Context.this;

            @Override
            public boolean hasNext() {
                return pointer != null && pointer.transformer != null;
            }

            @Override
            public Context next() {
                Context self = pointer;
                pointer = pointer.parent;
                return self;
            }
        };
    }

    public String prettyStash() {
        Map<String, Object> flatStash = new HashMap<>();
        for (Context context : this) {
            context.stash.forEach((key, value) -> {
                if (!flatStash.containsKey(key)) {
                    flatStash.put(key, value);
                }
            });
        }
        if (flatStash.isEmpty()) return "";
        return prettyPrint(flatStash, 0);
    }

    private String prettyPrint(Object object, int indentation) {

        if (object instanceof Map) {

            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append("{\n");

            ((Map<?, ?>) object)
                    .entrySet()
                    .stream()
                    .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                    .forEach(entry -> stringBuilder
                            .append(" ".repeat(indentation + INDENT))
                            .append(entry.getKey())
                            .append('=')
                            .append(prettyPrint(entry.getValue(), indentation + INDENT)));

            stringBuilder
                    .append(" ".repeat(indentation))
                    .append("}\n");

            return stringBuilder.toString();

        } else {
            return object.toString() + '\n';
        }
    }

    @Override
    public String toString() {
        return "Context { " + getDescription() + " }";
    }

    void handleException(Throwable exception) {
        Context pointer = this;
        do {
            if (pointer.exceptionListener != null) {
                pointer.exceptionListener.accept(this, exception);
            }
            if (pointer.syncedExceptionListener != null) {
                pointer.syncedExceptionListener.accept(this, exception);
            }
            pointer = pointer.parent;
        } while (pointer != null);
        exception.printStackTrace();  // TODO
    }

    private static Optional<Object> resolveDetail(Object object, String[] name) {
        for (int i = 1; i < name.length; i++) {  // root object has already been resolved, so start at 1
            if (object instanceof Map) object = ((Map<?, ?>) object).get(name[i]);
            else return Optional.empty();
        }
        return Optional.ofNullable(object);
    }

    public static class Builder {

        private final Context parent;
        private final Transformer<?, ?> transformer;
        private String description;
        private Map<String, Object> stash;
        private BiConsumer<Context, Throwable> exceptionListener;
        private BiConsumer<Context, Throwable> syncedExceptionListener;

        private Builder(Context parent, Transformer<?, ?> transformer) {
            this.parent = parent;
            this.transformer = transformer;
        }

        public Builder description(int description) {
            this.description = Integer.toString(description);
            return this;
        }

        public Builder description(long description) {
            this.description = Long.toString(description);
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder stash(Map<String, Object> stash) {
            this.stash = stash;
            return this;
        }

        public Builder onException(BiConsumer<Context, Throwable> exceptionListener) {
            this.exceptionListener = exceptionListener;
            return this;
        }

        public Builder onSyncedException(BiConsumer<Context, Throwable> syncedExceptionListener) {
            final Thread thread = Thread.currentThread();
            this.syncedExceptionListener = (context, exception) -> {
                if (Thread.currentThread() == thread) {
                    syncedExceptionListener.accept(context, exception);
                }
            };
            return this;
        }

        Context build() {
            return new Context(this);
        }
    }


    private static final Pattern STRING_FORMAT_SPECIFIER = Pattern.compile("\\{(.*?[^\\\\])}"); // TODO support escaping '{'

    public static class StringFormat {

        private final List<String> strings = new ArrayList<>();
        private final List<String[]> names = new ArrayList<>();
        private final boolean throwOnMissingItem;  // TODO

        public StringFormat(String format) {
            this(format, false);
        }

        public StringFormat(String format, boolean throwOnMissingItem) {
            this.throwOnMissingItem = throwOnMissingItem;
            Matcher matcher = STRING_FORMAT_SPECIFIER.matcher(format);
            int pointer = 0;
            while (matcher.find()) {
                strings.add(format.substring(pointer, matcher.start(0)));
                names.add(matcher.group(1).split("\\."));  // TODO use compiled pattern
                pointer = matcher.end(0);
            }
            strings.add(format.substring(pointer));

            // TODO unescape '{', '}' and '.'
        }

        public String format(Context context) {
            if (names.isEmpty()) return strings.get(0);
            StringBuilder stringBuilder = new StringBuilder();
            int i = 0;
            for (; i < names.size(); i++) {
                String[] name = names.get(i);
                stringBuilder
                        .append(strings.get(i))
                        .append(context.fetch(name)
                                .map(Object::toString)
                                .orElse(String.join(".", name)));  // TODO
            }
            stringBuilder.append(strings.get(i));
            return stringBuilder.toString();
        }
    }
}

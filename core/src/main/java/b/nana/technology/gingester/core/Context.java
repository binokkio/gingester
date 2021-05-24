package b.nana.technology.gingester.core;

import java.util.*;
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
    private final Map<String, Object> details;
    private final Function<Throwable, Boolean> exceptionListener;
    private final Function<Throwable, Boolean> syncedExceptionListener;

    private Context() {
        parent = null;
        depth = 0;
        transformer = null;
        description = null;
        details = Map.of();
        exceptionListener = null;
        syncedExceptionListener = null;
    }

    private Context(Builder builder) {
        parent = builder.parent != SEED ? builder.parent : null;
        depth = builder.parent != SEED ? builder.parent.depth + 1 : 0;
        transformer = builder.transformer;
        description = builder.description;
        details = builder.details != null ? builder.details : Collections.emptyMap();
        exceptionListener = builder.exceptionListener;
        syncedExceptionListener = builder.syncedExceptionListener;
    }

    public Context.Builder extend(Transformer<?, ?> transformer) {
        return new Context.Builder(this, transformer);
    }

    public void signal() {
        // TODO
    }

    public String[] getDescriptions() {
        Context pointer = this;
        String[] descriptions = new String[depth + 1];
        for (int i = descriptions.length - 1; i >= 0; i--) {
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

    public Optional<Object> getDetail(String name) {
        for (Context context : this) {
            Object detail = context.details.get(name);
            if (detail != null) return Optional.of(detail);
        }
        return Optional.empty();
    }

    public Map<String, Object> getDetails() {
        Map<String, Object> result = new HashMap<>();
        for (Context context : this) {
            context.details.forEach((key, value) -> {
                if (!result.containsKey(key)) {
                    result.put(key, value);
                }
            });
        }
        return result;
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

    public String prettyDetails() {
        Map<String, Object> details = getDetails();
        if (details.isEmpty()) return "";
        return prettyPrint(details, 0);
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
        return "Context { " + super.toString() + "," + getDescription() + "," + getDetails() + "}";
    }

    void handleException(Throwable exception) {
        Context pointer = this;
        do {
            boolean handled = false;
            if (pointer.exceptionListener != null) {
                handled = pointer.exceptionListener.apply(exception);
            }
            if (pointer.syncedExceptionListener != null) {
                handled |= pointer.syncedExceptionListener.apply(exception);
            }
            if (handled) return;
            pointer = pointer.parent;
        } while (pointer != null);
        exception.printStackTrace();  // TODO
    }

    public static class Builder {

        private final Context parent;
        private final Transformer<?, ?> transformer;
        private String description;
        private Map<String, Object> details;
        private Function<Throwable, Boolean> exceptionListener;
        private Function<Throwable, Boolean> syncedExceptionListener;

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

        public Builder details(Map<String, Object> details) {
            this.details = details;
            return this;
        }

        public Builder onException(Function<Throwable, Boolean> exceptionListener) {
            this.exceptionListener = exceptionListener;
            return this;
        }

        public Builder onSyncedException(Function<Throwable, Boolean> syncedExceptionListener) {
            final Thread thread = Thread.currentThread();
            this.syncedExceptionListener = exception -> {
                // TODO this misses the case where an a transformer is in its own downstream, maybe make that illegal
                if (Thread.currentThread() == thread) {
                    return syncedExceptionListener.apply(exception);
                } else {
                    return false;
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
        private final List<String[]> detailNames = new ArrayList<>();
        private final Function<String, String> sanitizer;
        private final boolean throwOnMissingDetail;  // TODO

        public StringFormat(String format) {
            this(format, s -> s);
        }

        public StringFormat(String format, Function<String, String> sanitizer) {
            this(format, sanitizer, false);
        }

        public StringFormat(String format, Function<String, String> sanitizer, boolean throwOnMissingDetail) {
            this.sanitizer = sanitizer;
            this.throwOnMissingDetail = throwOnMissingDetail;
            Matcher matcher = STRING_FORMAT_SPECIFIER.matcher(format);
            int pointer = 0;
            while (matcher.find()) {
                strings.add(format.substring(pointer, matcher.start(0)));
                detailNames.add(matcher.group(1).split("\\."));  // TODO use compiled pattern
                pointer = matcher.end(0);
            }
            strings.add(format.substring(pointer));

            // TODO unescape '{', '}' and '.'
        }

        public String format(Context context) {
            StringBuilder stringBuilder = new StringBuilder();
            int i = 0;
            for (; i < detailNames.size(); i++) {
                String[] detailName = detailNames.get(i);
                stringBuilder
                        .append(strings.get(i))
                        .append(resolveDetail(context, detailName)
                                .map(Object::toString)
                                .map(sanitizer)
                                .orElse(String.join(".", detailName)));  // TODO
            }
            stringBuilder.append(strings.get(i));
            return stringBuilder.toString();
        }

        private Optional<Object> resolveDetail(Context context, String[] detailName) {
            Object detail = context.getDetail(detailName[0]).orElse(null);
            for (int i = 1; i < detailName.length; i++) {
                if (!(detail instanceof Map)) return Optional.empty();
                detail = ((Map<?, ?>) detail).get(detailName[i]);
            }
            return Optional.ofNullable(detail);
        }
    }
}

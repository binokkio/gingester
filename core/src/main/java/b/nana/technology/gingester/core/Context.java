package b.nana.technology.gingester.core;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Context implements Iterable<Context> {

    static final Context SEED = new Context();

    final Context parent;
    final int depth;
    final Transformer<?, ?> transformer;
    final String description;
    private final Map<String, Object> details;
    private final Consumer<Throwable> exceptionListener;

    private Context() {
        parent = null;
        depth = 0;
        transformer = null;
        description = null;
        details = Map.of();
        exceptionListener = null;
    }

    private Context(Builder builder) {
        parent = builder.parent != SEED ? builder.parent : null;
        depth = builder.parent != SEED ? builder.parent.depth + 1 : 0;
        transformer = builder.transformer;
        description = builder.description;
        details = builder.details != null ? builder.details : Collections.emptyMap();
        exceptionListener = builder.exceptionListener;
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

    @Override
    public String toString() {
        return "Context { " + super.toString() + " :: " + getDescription() + " }";
    }

    void handleException(Throwable exception) {
        Context pointer = this;
        do {
            if (pointer.exceptionListener != null) {
                pointer.exceptionListener.accept(exception);
            }
            pointer = pointer.parent;
        } while (pointer != null);
        exception.printStackTrace();  // TODO
    }

    public static class Builder {

        private final Context parent;
        private final Transformer<?, ?> transformer;
        private String description;
        private Map<String, Object> details;
        private Consumer<Throwable> exceptionListener;

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

        public Builder onException(Consumer<Throwable> exceptionListener) {
            this.exceptionListener = exceptionListener;
            return this;
        }

        Context build() {
            return new Context(this);
        }
    }


    private static final Pattern STRING_FORMAT_SPECIFIER = Pattern.compile("\\{(.*?[^\\\\])}");

    public static class StringFormat {

        private final List<String> strings = new ArrayList<>();
        private final List<String> detailNames = new ArrayList<>();

        public StringFormat(String format) {
            Matcher matcher = STRING_FORMAT_SPECIFIER.matcher(format);
            int pointer = 0;
            while (matcher.find()) {
                strings.add(format.substring(pointer, matcher.start(0)));
                detailNames.add(matcher.group(1));
                pointer = matcher.end(0);
            }
            strings.add(format.substring(pointer));
        }

        public String format(Context context) {
            StringBuilder stringBuilder = new StringBuilder();
            int i = 0;
            for (; i < detailNames.size(); i++) {
                String detailName = detailNames.get(i);
                stringBuilder
                        .append(strings.get(i))
                        .append(context.getDetail(detailNames.get(i)).orElse(detailName));
            }
            stringBuilder.append(strings.get(i));
            return stringBuilder.toString();
        }
    }
}

package b.nana.technology.gingester.core;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class Context implements Iterable<Context> {

    static final Context SEED = new Context();

    final Context parent;
    final int depth;
    final Transformer<?, ?> transformer;
    final String description;
    private final List<Object> attachments;
    private final Consumer<Throwable> exceptionListener;

    private Context() {
        parent = null;
        depth = 0;
        transformer = null;
        description = null;
        attachments = null;
        exceptionListener = null;
    }

    private Context(Builder builder) {
        parent = builder.parent != SEED ? builder.parent : null;
        depth = builder.parent != SEED ? builder.parent.depth + 1 : 0;
        transformer = builder.transformer;
        description = builder.description;
        attachments = builder.attachments != null ?
                Collections.unmodifiableList(builder.attachments) :
                Collections.emptyList();
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

    public List<Object> getAttachments(Class<? extends Transformer<?, ?>> fromClass) {
        if (this != SEED) {
            for (Context context : this) {
                if (context.transformer.getClass().equals(fromClass)) {
                    return context.attachments;
                }
            }
        }
        return List.of();
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
        private List<Object> attachments;
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

        public Builder attachment(Object attachment) {
            if (attachments == null) {
                attachments = new ArrayList<>();
            }
            attachments.add(attachment);
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
}

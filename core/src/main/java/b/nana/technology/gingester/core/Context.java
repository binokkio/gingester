package b.nana.technology.gingester.core;

import java.util.*;

public final class Context implements Iterable<Context> {

    static final Context SEED = new Context();

    final Context parent;
    final Transformer<?, ?> transformer;
    final String description;
    final List<Object> attachments;

    private Context() {
        parent = null;
        transformer = null;
        description = null;
        attachments = null;
    }

    private Context(Builder builder) {
        this.parent = builder.parent != SEED ? builder.parent : null;
        this.transformer = builder.transformer;
        this.description = builder.description;
        this.attachments = builder.attachments != null ?
                Collections.unmodifiableList(builder.attachments) :
                Collections.emptyList();
    }

    public Context.Builder extend(Transformer<?, ?> transformer) {
        return new Context.Builder(this, transformer);
    }

    public void signal() {
        // TODO
    }

    @Override
    public Iterator<Context> iterator() {
        return new Iterator<>() {

            private Context pointer = Context.this;

            @Override
            public boolean hasNext() {
                return pointer.parent != null;
            }

            @Override
            public Context next() {
                pointer = pointer.parent;
                return pointer;
            }
        };
    }

    public String getDescription() {
        List<String> descriptions = new LinkedList<>();
        Context pointer = this;
        do {
            String description = pointer.description;
            if (description != null) descriptions.add(0, description);
            pointer = pointer.parent;
        } while (pointer != null);
        return String.join(" :: ", descriptions);
    }

    @Override
    public String toString() {
        return "Context { " + super.toString() + " :: " + getDescription() + " }";
    }

    public static class Builder {

        private final Context parent;
        private final Transformer<?, ?> transformer;
        private String description;
        private List<Object> attachments;

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

        Context build() {
            return new Context(this);
        }
    }
}

package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.link.ExceptionLink;
import b.nana.technology.gingester.core.link.NormalLink;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

final class Builder implements Gingester.Builder {

    final Set<Transformer<?, ?>> transformers = new LinkedHashSet<>();
    boolean report;

    private boolean built;

    @Override
    public Builder report(boolean report) {
        this.report = report;
        return this;
    }

    @Override
    public void add(Transformer<?, ?> transformer) {
        transformers.add(transformer);
    }

    @Override
    public Transformer<?, ?> getTransformer(String name) {
        return transformers.stream()
                .filter(transformer -> transformer.getName().orElseGet(() -> Provider.name(transformer)).equals(name))
                .reduce((a, b) -> {
                    throw new IllegalStateException("Multiple matches for " + name);
                })
                .orElseThrow(() -> new NoSuchElementException("No transformer named " + name));
    }

    @SuppressWarnings("unchecked")  // checked at runtime
    @Override
    public <T extends Transformer<?, ?>> T getTransformer(String name, Class<T> transformerClass) {
        T transformer = (T) getTransformer(name);
        if (!transformerClass.isInstance(transformer)) throw new ClassCastException();  // TODO
        return transformer;
    }

    @Override
    public void name(String name, Transformer<?, ?> transformer) {
        if (transformer.name != null) throw new IllegalArgumentException("Transformer was already named");
        if (transformers.stream().map(Transformer::getName).flatMap(Optional::stream).anyMatch(name::equals))
            throw new IllegalArgumentException("Transformer name not unique: " + name);
        transformer.name = name;
        add(transformer);
    }

    @Override
    public NormalLink<?> link(String fromName, String toName) {
        return linkUnchecked(getTransformer(fromName), getTransformer(toName));
    }

    @SuppressWarnings("unchecked")
        // checked at runtime in link()
    <T> NormalLink<T> linkUnchecked(Transformer<?, ?> from, Transformer<?, ?> to) {
        return link((Transformer<?, T>) from, (Transformer<? super T, ?>) to);
    }

    @Override
    public <T> NormalLink<T> link(Transformer<?, T> from, Transformer<? super T, ?> to) {
        add(from);
        add(to);
        from.assertLinkToWouldNotBeCircular(to);
        from.assertLinkToWouldBeCompatible(to);
        NormalLink<T> link = new NormalLink<>(from, to);
        from.outgoing.add(link);
        to.incoming.add(link);
        return link;
    }

    @Override
    public <T> NormalLink<T> link(Transformer<?, T> from, Consumer<? super T> consumer) {
        return link(from, new Transformer<>(from.outputClass, Void.class) {
            @Override
            protected void transform(Context context, T input) {
                consumer.accept(input);
            }
        });
    }

    @Override
    public <T> NormalLink<T> link(Transformer<?, T> from, BiConsumer<Context, ? super T> consumer) {
        return link(from, new Transformer<>(from.outputClass, Void.class) {
            @Override
            protected void transform(Context context, T input) {
                consumer.accept(context, input);
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public ExceptionLink except(String fromName, String toName) {
        Transformer<Throwable, ?> to = (Transformer<Throwable, ?>) getTransformer(toName);
        if (!to.inputClass.equals(Throwable.class)) throw new IllegalArgumentException("");  // TODO
        return except(getTransformer(fromName), to);
    }

    @Override
    public ExceptionLink except(Transformer<?, ?> from, Transformer<Throwable, ?> to) {
        add(from);
        add(to);
        from.assertLinkToWouldNotBeCircular(to);
        ExceptionLink link = new ExceptionLink(from, to);
        from.exceptionHandler = link;
        to.incoming.add(link);
        return link;
    }

    @Override
    public void sync(String fromName, String toName) {
        sync(getTransformer(fromName), getTransformer(toName));
    }

    @Override
    public void sync(Transformer<?, ?> from, Transformer<?, ?> to) {

        List<ArrayDeque<Transformer<?, ?>>> routes = from.getDownstreamRoutes().stream()
                .filter(route -> route.getLast() == to)
                .collect(Collectors.toList());

        if (routes.isEmpty()) {
            throw new IllegalStateException("No route between given transformers");
        }

        from.syncs.add(to);

        Set<Transformer<?, ?>> sanity = routes.stream().map(route ->
                route.stream().reduce((f, t) -> {
                    f.outgoing.stream().filter(l -> l.to == t).findFirst().orElseThrow().requireSync();
                    return t;
                }).orElseThrow()
        ).collect(Collectors.toSet());

        if (!sanity.equals(Set.of(to))) {
            throw new IllegalStateException();  // TODO
        }
    }

    @Override
    public <T> void seed(Transformer<T, ?> transformer, T seed) {
        add(transformer);
        transformer.queue.add(new Batch<>(Context.SEED, seed));
    }

    @Override
    public final Gingester build() {

        if (built) throw new IllegalStateException("Already built");
        built = true;

        // parameter based links
        for (Transformer<?, ?> transformer : transformers) {
            if (transformer.outgoing.isEmpty()) {
                for (String to : transformer.getLinks()) {
                    linkUnchecked(transformer, getTransformer(to)).markImplied();
                }
            }
        }

        // seed all transformers that have no inputs and were not already seeded
        for (Transformer<?, ?> transformer : transformers) {
            if (transformer.isEmpty() && transformer.incoming.isEmpty()) {
                seed(transformer, null);
            }
        }

        Map<String, Integer> counters = new HashMap<>();

        // update every transformer name with a counter suffix
        for (Transformer<?, ?> transformer : transformers) {
            String name = transformer.getName().orElse(Provider.name(transformer));
            Integer counter = counters.get(name);
            counter = counter == null ? 1 : counter + 1;
            counters.put(name, counter);
            transformer.name = name + "-" + counter;
        }

        // remove the counter suffix from transformers with unique names
        for (Transformer<?, ?> transformer : transformers) {
            String name = transformer.getName().orElseThrow();
            if (name.endsWith("-1")) {
                String nameWithoutSuffix = name.substring(0, name.length() - 2);
                if (counters.get(nameWithoutSuffix) == 1) {
                    transformer.name = nameWithoutSuffix;
                }
            }
        }

        return new Gingester(this);
    }
}

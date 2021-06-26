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
    public Transformer<?, ?> getTransformer(String id) {
        return transformers.stream()
                .filter(transformer -> transformer.getId().orElseGet(() -> Provider.name(transformer)).equals(id))
                .reduce((a, b) -> { throw new IllegalStateException("Multiple matches for " + id); })
                .orElseThrow(() -> new NoSuchElementException("No transformer with id " + id));
    }

    @SuppressWarnings("unchecked")  // checked at runtime
    @Override
    public <T extends Transformer<?, ?>> T getTransformer(String id, Class<T> transformerClass) {
        T transformer = (T) getTransformer(id);
        if (!transformerClass.isInstance(transformer)) throw new ClassCastException();  // TODO
        return transformer;
    }

    @Override
    public void id(String id, Transformer<?, ?> transformer) {
        if (transformer.id != null) throw new IllegalArgumentException("Transformer was already given an id");
        if (transformers.stream().map(Transformer::getId).flatMap(Optional::stream).anyMatch(id::equals))
            throw new IllegalArgumentException("Transformer id not unique: " + id);
        transformer.id = id;
        add(transformer);
    }

    @Override
    public NormalLink<?> link(String fromId, String toId) {
        return linkUnchecked(getTransformer(fromId), getTransformer(toId));
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
    public ExceptionLink except(String fromId, String toId) {
        Transformer<Throwable, ?> to = (Transformer<Throwable, ?>) getTransformer(toId);
        if (!to.inputClass.isAssignableFrom(Throwable.class)) throw new IllegalArgumentException("");  // TODO
        return except(getTransformer(fromId), to);
    }

    @Override
    public ExceptionLink except(Transformer<?, ?> from, Transformer<Throwable, ?> to) {
        add(from);
        add(to);
        from.assertLinkToWouldNotBeCircular(to);
        ExceptionLink link = new ExceptionLink(from, to);
        from.excepts.add(link);
        to.incoming.add(link);
        return link;
    }

    @Override
    public void sync(String fromId, String toId) {
        sync(getTransformer(fromId), getTransformer(toId));
    }

    @Override
    public void sync(Transformer<?, ?> from, Transformer<?, ?> to) {
        from.syncs.add(to);
    }

    @Override
    public <T> void seed(Transformer<T, ?> transformer, T seed) {
        add(transformer);
        transformer.queue.add(new Batch<>(Context.SEED, seed));
    }

    public <T> void seed(Transformer<T, ?> transformer, Context.Builder contextBuilder, T seed) {
        add(transformer);
        transformer.queue.add(new Batch<>(contextBuilder.build(), seed));
    }

    @Override
    public final Gingester build() {

        if (built) throw new IllegalStateException("Already built");
        built = true;

        // parameter based links
        for (Transformer<?, ?> transformer : transformers) {
            if (transformer.outgoing.isEmpty()) {  // TODO this ignores the exception handler, good or bad?
                for (String to : transformer.getLinks()) {
                    linkUnchecked(transformer, getTransformer(to)).markImplied();
                }
            }
        }

        // syncs
        for (Transformer<?, ?> from : transformers) {
            for (Transformer<?, ?> to : from.syncs) {
                List<ArrayDeque<Transformer<?, ?>>> routes = from.getDownstreamRoutes().stream()
                        .filter(route -> route.getLast() == to)
                        .collect(Collectors.toList());

                if (routes.isEmpty()) {
                    throw new IllegalStateException("No route between given transformers");
                }

                Set<Transformer<?, ?>> sanity = routes.stream().map(route ->
                        route.stream().reduce((f, t) -> {
                            f.getOutgoing().stream().filter(l -> l.to == t).findFirst().orElseThrow().requireSync();
                            return t;
                        }).orElseThrow()
                ).collect(Collectors.toSet());

                if (!sanity.equals(Set.of(to))) {
                    throw new IllegalStateException();  // TODO
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

        // update every transformer id with a counter suffix
        for (Transformer<?, ?> transformer : transformers) {
            String id = transformer.getId().orElse(Provider.name(transformer));
            Integer counter = counters.get(id);
            counter = counter == null ? 1 : counter + 1;
            counters.put(id, counter);
            transformer.id = id + "-" + counter;
        }

        // remove the counter suffix from transformers with unique ids
        for (Transformer<?, ?> transformer : transformers) {
            String id = transformer.getId().orElseThrow();
            if (id.endsWith("-1")) {
                String idWithoutSuffix = id.substring(0, id.length() - 2);
                if (counters.get(idWithoutSuffix) == 1) {
                    transformer.id = idWithoutSuffix;
                }
            }
        }

        return new Gingester(this);
    }
}

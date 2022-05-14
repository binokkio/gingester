package b.nana.technology.gingester.core.configuration;

import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformer.TransformerFactory;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public final class TransformerConfiguration extends BaseConfiguration<TransformerConfiguration> {

    private static final Set<String> RESERVED_IDS = Set.of("__seed__", "__elog__");

    private String id;
    private String name;
    private Transformer<?, ?> transformer;
    private Boolean report;
    private boolean isNeverMaybeNext;



    public TransformerConfiguration() {
        links(Collections.singletonList("__maybe_next__"));
    }



    public TransformerConfiguration id(String id) {

        if (!Character.isUpperCase(id.charAt(0)) && !RESERVED_IDS.contains(id))
            throw new IllegalArgumentException("Transformer id must start with an uppercase character: " + id);

        this.id = id;
        return this;
    }

    public TransformerConfiguration name(String name) {
        this.name = name;
        return this;
    }

    public TransformerConfiguration transformer(Transformer<?, ?> transformer) {
        this.transformer = transformer;
        return this;
    }

    public TransformerConfiguration transformer(String name, Transformer<?, ?> transformer) {
        this.name = name;
        this.transformer = transformer;
        return this;
    }

    public TransformerConfiguration report(boolean report) {
        this.report = report;
        return this;
    }

    public TransformerConfiguration isNeverMaybeNext(boolean isNeverMaybeNext) {
        this.isNeverMaybeNext = isNeverMaybeNext;
        return this;
    }


    // TODO make not optional (with a requireNonNull and hasId() helper)
    public Optional<String> getId() {
        return Optional.ofNullable(id);
    }

    // TODO make not optional, move name and transformer to constructor?
    public Optional<String> getName() {
        if (name != null) {
            return Optional.of(name);
        } else {
            return getTransformer().map(TransformerFactory::getUniqueName);
        }
    }

    public Optional<Transformer<?, ?>> getTransformer() {
        return Optional.ofNullable(transformer);
    }

    public Optional<Boolean> getReport() {
        return Optional.ofNullable(report);
    }

    public boolean isNeverMaybeNext() {
        return isNeverMaybeNext;
    }
}

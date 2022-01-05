package b.nana.technology.gingester.core.configuration;

import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformer.TransformerFactory;

import java.util.Collections;
import java.util.Optional;

public final class TransformerConfiguration extends BaseConfiguration<TransformerConfiguration> {

    private String id;
    private String name;
    private Transformer<?, ?> transformer;
    private Boolean report;



    public TransformerConfiguration() {
        links(Collections.singletonList("__maybe_next__"));
    }



    public TransformerConfiguration id(String id) {
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



    public Optional<String> getId() {
        return Optional.ofNullable(id);
    }

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
}

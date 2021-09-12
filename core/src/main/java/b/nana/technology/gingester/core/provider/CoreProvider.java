package b.nana.technology.gingester.core.provider;

import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformers.Fetch;
import b.nana.technology.gingester.core.transformers.Seed;
import b.nana.technology.gingester.core.transformers.Stash;
import b.nana.technology.gingester.core.transformers.Swap;

import java.util.Collection;
import java.util.List;

public final class CoreProvider implements Provider {

    @Override
    public Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses() {
        return List.of(
                Fetch.class,
                Seed.class,
                Stash.class,
                Swap.class
        );
    }
}

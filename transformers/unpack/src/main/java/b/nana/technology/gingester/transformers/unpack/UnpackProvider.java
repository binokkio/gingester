package b.nana.technology.gingester.transformers.unpack;

import b.nana.technology.gingester.core.provider.Provider;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.Collection;
import java.util.List;

public final class UnpackProvider implements Provider {

    @Override
    public Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses() {
        return List.of(
                Compress.class,
                Pack.class,
                Unpack.class
        );
    }
}

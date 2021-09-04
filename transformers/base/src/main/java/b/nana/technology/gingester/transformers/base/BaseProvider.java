package b.nana.technology.gingester.transformers.base;

import b.nana.technology.gingester.core.provider.Provider;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.transformers.std.Out;
import b.nana.technology.gingester.transformers.base.transformers.string.Append;
import b.nana.technology.gingester.transformers.base.transformers.string.Generate;

import java.util.Collection;
import java.util.List;

public class BaseProvider implements Provider {
    @Override
    public Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses() {
        return List.of(
                Append.class,
                Generate.class,
                Out.class
        );
    }
}

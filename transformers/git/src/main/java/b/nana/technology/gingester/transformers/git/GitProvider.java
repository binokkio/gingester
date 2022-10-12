package b.nana.technology.gingester.transformers.git;

import b.nana.technology.gingester.core.provider.Provider;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.git.giterate.Giterate;

import java.util.Collection;
import java.util.List;

public final class GitProvider implements Provider {

    @Override
    public Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses() {
        return List.of(
                Giterate.class
        );
    }
}

package b.nana.technology.gingester.core.provider;

import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformers.Void;
import b.nana.technology.gingester.core.transformers.*;
import b.nana.technology.gingester.core.transformers.groupby.CountModulo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class CoreProvider implements Provider {

    @Override
    public Map<String, String> getCaseHints() {
        return Map.of(
                "groupby", "GroupBy"
        );
    }

    @Override
    public Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses() {
        return List.of(
                CountModulo.class,
                Fetch.class,
                FetchAll.class,
                Log.class,
                Merge.class,
                Monkey.class,
                OnFinish.class,
                OnPrepare.class,
                Passthrough.class,
                Repeat.class,
                Stash.class,
                Swap.class,
                Throw.class,
                Void.class
        );
    }
}

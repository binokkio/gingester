package b.nana.technology.gingester.core.provider;

import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformers.Void;
import b.nana.technology.gingester.core.transformers.*;
import b.nana.technology.gingester.core.transformers.passthrough.Passthrough;
import b.nana.technology.gingester.core.transformers.stash.*;

import java.util.Collection;
import java.util.List;

public final class CoreProvider implements Provider {

    @Override
    public Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses() {
        return List.of(
                CycleRoute.class,
                Fetch.class,
                FetchAll.class,
                FetchObject.class,
                FinishGate.class,
                Log.class,
                Merge.class,
                Monkey.class,
                OnFinish.class,
                OnPrepare.class,
                OrdinalRoute.class,
                Passthrough.class,
                Repeat.class,
                Stash.class,
                StashString.class,
                Swap.class,
                Throw.class,
                Void.class
        );
    }
}

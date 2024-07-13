package b.nana.technology.gingester.core.provider;

import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.Collection;
import java.util.List;

public final class CoreProvider implements Provider {

    @Override
    public Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses() {
        return List.of(
                b.nana.technology.gingester.core.transformers.As.class,
                b.nana.technology.gingester.core.transformers.CliToGraphTxt.class,
                b.nana.technology.gingester.core.transformers.CycleRoute.class,
                b.nana.technology.gingester.core.transformers.FinishGate.class,
                b.nana.technology.gingester.core.transformers.Gcli.class,
                b.nana.technology.gingester.core.transformers.Log.class,
                b.nana.technology.gingester.core.transformers.Merge.class,
                b.nana.technology.gingester.core.transformers.Monkey.class,
                b.nana.technology.gingester.core.transformers.OnFinish.class,
                b.nana.technology.gingester.core.transformers.OnFinishFetch.class,
                b.nana.technology.gingester.core.transformers.OnPrepare.class,
                b.nana.technology.gingester.core.transformers.OrdinalRoute.class,
                b.nana.technology.gingester.core.transformers.Probe.class,
                b.nana.technology.gingester.core.transformers.passthrough.Passthrough.class,
                b.nana.technology.gingester.core.transformers.primitive.BooleanDef.class,
                b.nana.technology.gingester.core.transformers.primitive.BooleanToString.class,
                b.nana.technology.gingester.core.transformers.primitive.DoubleDef.class,
                b.nana.technology.gingester.core.transformers.primitive.DoubleToString.class,
                b.nana.technology.gingester.core.transformers.primitive.FloatDef.class,
                b.nana.technology.gingester.core.transformers.primitive.FloatToString.class,
                b.nana.technology.gingester.core.transformers.primitive.IntDef.class,
                b.nana.technology.gingester.core.transformers.primitive.IntToString.class,
                b.nana.technology.gingester.core.transformers.primitive.LongDef.class,
                b.nana.technology.gingester.core.transformers.primitive.LongToString.class,
                b.nana.technology.gingester.core.transformers.RatioRoute.class,
                b.nana.technology.gingester.core.transformers.Repeat.class,
                b.nana.technology.gingester.core.transformers.stash.FetchAll.class,
                b.nana.technology.gingester.core.transformers.stash.Fetch.class,
                b.nana.technology.gingester.core.transformers.stash.FetchMap.class,
                b.nana.technology.gingester.core.transformers.stash.FetchObject.class,
                b.nana.technology.gingester.core.transformers.stash.Stash.class,
                b.nana.technology.gingester.core.transformers.stash.StashString.class,
                b.nana.technology.gingester.core.transformers.stash.Swap.class,
                b.nana.technology.gingester.core.transformers.string.Def.class,
                b.nana.technology.gingester.core.transformers.Throw.class,
                b.nana.technology.gingester.core.transformers.Void.class,
                b.nana.technology.gingester.core.transformers.wormhole.In.class,
                b.nana.technology.gingester.core.transformers.wormhole.Out.class
        );
    }
}

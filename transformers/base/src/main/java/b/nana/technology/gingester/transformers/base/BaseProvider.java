package b.nana.technology.gingester.transformers.base;

import b.nana.technology.gingester.core.provider.Provider;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.Collection;
import java.util.List;

public class BaseProvider implements Provider {

    @Override
    public Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses() {
        return List.of(
                b.nana.technology.gingester.transformers.base.transformers.dsv.FromJson.class,
                b.nana.technology.gingester.transformers.base.transformers.dsv.ToJson.class,
                b.nana.technology.gingester.transformers.base.transformers.inputstream.Drain.class,
                b.nana.technology.gingester.transformers.base.transformers.inputstream.Gunzip.class,
                b.nana.technology.gingester.transformers.base.transformers.inputstream.Split.class,
                b.nana.technology.gingester.transformers.base.transformers.inputstream.ToBytes.class,
                b.nana.technology.gingester.transformers.base.transformers.inputstream.ToJson.class,
                b.nana.technology.gingester.transformers.base.transformers.json.Copy.class,
                b.nana.technology.gingester.transformers.base.transformers.json.ObjectToArray.class,
                b.nana.technology.gingester.transformers.base.transformers.json.Path.class,
                b.nana.technology.gingester.transformers.base.transformers.json.ReplaceBytesWithPaths.class,
                b.nana.technology.gingester.transformers.base.transformers.json.ToInputStream.class,
                b.nana.technology.gingester.transformers.base.transformers.json.Wrap.class,
                b.nana.technology.gingester.transformers.base.transformers.path.Move.class,
                b.nana.technology.gingester.transformers.base.transformers.path.Open.class,
                b.nana.technology.gingester.transformers.base.transformers.path.Search.class,
                b.nana.technology.gingester.transformers.base.transformers.path.Write.class,
                b.nana.technology.gingester.transformers.base.transformers.resource.Open.class,
                b.nana.technology.gingester.transformers.base.transformers.std.Out.class,
                b.nana.technology.gingester.transformers.base.transformers.string.Append.class,
                b.nana.technology.gingester.transformers.base.transformers.string.Generate.class
        );
    }
}

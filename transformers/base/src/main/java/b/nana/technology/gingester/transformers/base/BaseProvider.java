package b.nana.technology.gingester.transformers.base;

import b.nana.technology.gingester.core.provider.Provider;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.Collection;
import java.util.List;

public class BaseProvider implements Provider {

    @Override
    public Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses() {
        return List.of(
                b.nana.technology.gingester.transformers.base.transformers.bytes.ToInputStream.class,
                b.nana.technology.gingester.transformers.base.transformers.bytes.ToString.class,
                b.nana.technology.gingester.transformers.base.transformers.cron.Job.class,
                b.nana.technology.gingester.transformers.base.transformers.dsv.FromJson.class,
                b.nana.technology.gingester.transformers.base.transformers.dsv.ToJson.class,
                b.nana.technology.gingester.transformers.base.transformers.exec.Exec.class,
                b.nana.technology.gingester.transformers.base.transformers.http.Get.class,
                b.nana.technology.gingester.transformers.base.transformers.inputstream.Drain.class,
                b.nana.technology.gingester.transformers.base.transformers.inputstream.Gunzip.class,
                b.nana.technology.gingester.transformers.base.transformers.inputstream.Split.class,
                b.nana.technology.gingester.transformers.base.transformers.inputstream.ToBytes.class,
                b.nana.technology.gingester.transformers.base.transformers.inputstream.ToJson.class,
                b.nana.technology.gingester.transformers.base.transformers.json.AsBoolean.class,
                b.nana.technology.gingester.transformers.base.transformers.json.AsDouble.class,
                b.nana.technology.gingester.transformers.base.transformers.json.AsFloat.class,
                b.nana.technology.gingester.transformers.base.transformers.json.AsInteger.class,
                b.nana.technology.gingester.transformers.base.transformers.json.AsLong.class,
                b.nana.technology.gingester.transformers.base.transformers.json.AsText.class,
                b.nana.technology.gingester.transformers.base.transformers.json.Copy.class,
                b.nana.technology.gingester.transformers.base.transformers.json.ObjectToArray.class,
                b.nana.technology.gingester.transformers.base.transformers.json.Path.class,
                b.nana.technology.gingester.transformers.base.transformers.json.ReplaceBytesWithPaths.class,
                b.nana.technology.gingester.transformers.base.transformers.json.Stream.class,
                b.nana.technology.gingester.transformers.base.transformers.json.ToBytes.class,
                b.nana.technology.gingester.transformers.base.transformers.json.Wrap.class,
                b.nana.technology.gingester.transformers.base.transformers.object.ToString.class,
                b.nana.technology.gingester.transformers.base.transformers.path.LastModified.class,
                b.nana.technology.gingester.transformers.base.transformers.path.Link.class,
                b.nana.technology.gingester.transformers.base.transformers.path.Move.class,
                b.nana.technology.gingester.transformers.base.transformers.path.Open.class,
                b.nana.technology.gingester.transformers.base.transformers.path.Search.class,
                b.nana.technology.gingester.transformers.base.transformers.path.Size.class,
                b.nana.technology.gingester.transformers.base.transformers.path.Write.class,
                b.nana.technology.gingester.transformers.base.transformers.regex.Find.class,
                b.nana.technology.gingester.transformers.base.transformers.regex.Group.class,
                b.nana.technology.gingester.transformers.base.transformers.regex.Replace.class,
                b.nana.technology.gingester.transformers.base.transformers.resource.Open.class,
                b.nana.technology.gingester.transformers.base.transformers.std.In.class,
                b.nana.technology.gingester.transformers.base.transformers.std.Out.class,
                b.nana.technology.gingester.transformers.base.transformers.string.Append.class,
                b.nana.technology.gingester.transformers.base.transformers.string.Create.class,
                b.nana.technology.gingester.transformers.base.transformers.string.ToBytes.class,
                b.nana.technology.gingester.transformers.base.transformers.time.FromMillis.class,
                b.nana.technology.gingester.transformers.base.transformers.time.FromSeconds.class,
                b.nana.technology.gingester.transformers.base.transformers.time.FromString.class,
                b.nana.technology.gingester.transformers.base.transformers.time.Now.class,
                b.nana.technology.gingester.transformers.base.transformers.time.ToString.class,
                b.nana.technology.gingester.transformers.base.transformers.util.Sample.class,
                b.nana.technology.gingester.transformers.base.transformers.util.Throttle.class
        );
    }
}

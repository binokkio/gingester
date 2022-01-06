package b.nana.technology.gingester.transformers.base;

import b.nana.technology.gingester.core.provider.Provider;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class BaseProvider implements Provider {

    @Override
    public Map<String, String> getCaseHints() {
        return Map.of(
                "bigdecimal", "BigDecimal",
                "inputstream", "InputStream"
        );
    }

    @Override
    public Collection<Class<? extends Transformer<?, ?>>> getTransformerClasses() {
        return List.of(
                b.nana.technology.gingester.transformers.base.transformers.bigdecimal.Multiply.class,
                b.nana.technology.gingester.transformers.base.transformers.bytes.NotEmpty.class,
                b.nana.technology.gingester.transformers.base.transformers.bytes.ToInputStream.class,
                b.nana.technology.gingester.transformers.base.transformers.bytes.ToJson.class,
                b.nana.technology.gingester.transformers.base.transformers.bytes.ToString.class,
                b.nana.technology.gingester.transformers.base.transformers.cron.Job.class,
                b.nana.technology.gingester.transformers.base.transformers.dsv.FromJson.class,
                b.nana.technology.gingester.transformers.base.transformers.dsv.ToJson.class,
                b.nana.technology.gingester.transformers.base.transformers.exec.Exec.class,
                b.nana.technology.gingester.transformers.base.transformers.html.ElementsContainingOwnText.class,
                b.nana.technology.gingester.transformers.base.transformers.html.ElementsContainingText.class,
                b.nana.technology.gingester.transformers.base.transformers.html.FromString.class,
                b.nana.technology.gingester.transformers.base.transformers.html.NextElementSibling.class,
                b.nana.technology.gingester.transformers.base.transformers.html.Select.class,
                b.nana.technology.gingester.transformers.base.transformers.html.ToString.class,
                b.nana.technology.gingester.transformers.base.transformers.http.Get.class,
                b.nana.technology.gingester.transformers.base.transformers.index.Index.class,
                b.nana.technology.gingester.transformers.base.transformers.index.Lookup.class,
                b.nana.technology.gingester.transformers.base.transformers.index.Stream.class,
                b.nana.technology.gingester.transformers.base.transformers.inputstream.Append.class,
                b.nana.technology.gingester.transformers.base.transformers.inputstream.Drain.class,
                b.nana.technology.gingester.transformers.base.transformers.inputstream.FromOutputStream.class,
                b.nana.technology.gingester.transformers.base.transformers.inputstream.Gunzip.class,
                b.nana.technology.gingester.transformers.base.transformers.inputstream.Join.class,
                b.nana.technology.gingester.transformers.base.transformers.inputstream.Pipe.class,
                b.nana.technology.gingester.transformers.base.transformers.inputstream.Prepend.class,
                b.nana.technology.gingester.transformers.base.transformers.inputstream.Skip.class,
                b.nana.technology.gingester.transformers.base.transformers.inputstream.Split.class,
                b.nana.technology.gingester.transformers.base.transformers.inputstream.SplitLines.class,
                b.nana.technology.gingester.transformers.base.transformers.inputstream.ToBytes.class,
                b.nana.technology.gingester.transformers.base.transformers.inputstream.ToJson.class,
                b.nana.technology.gingester.transformers.base.transformers.inputstream.ToOutputStream.class,
                b.nana.technology.gingester.transformers.base.transformers.inputstream.ToString.class,
                b.nana.technology.gingester.transformers.base.transformers.json.AsBigDecimal.class,
                b.nana.technology.gingester.transformers.base.transformers.json.AsBoolean.class,
                b.nana.technology.gingester.transformers.base.transformers.json.AsDouble.class,
                b.nana.technology.gingester.transformers.base.transformers.json.AsFloat.class,
                b.nana.technology.gingester.transformers.base.transformers.json.AsInteger.class,
                b.nana.technology.gingester.transformers.base.transformers.json.AsLong.class,
                b.nana.technology.gingester.transformers.base.transformers.json.AsText.class,
                b.nana.technology.gingester.transformers.base.transformers.json.Copy.class,
                b.nana.technology.gingester.transformers.base.transformers.json.Create.class,
                b.nana.technology.gingester.transformers.base.transformers.json.ForceArrays.class,
                b.nana.technology.gingester.transformers.base.transformers.json.Internpret.class,
                b.nana.technology.gingester.transformers.base.transformers.json.ObjectToArray.class,
                b.nana.technology.gingester.transformers.base.transformers.json.Path.class,
                b.nana.technology.gingester.transformers.base.transformers.json.Remove.class,
                b.nana.technology.gingester.transformers.base.transformers.json.ReplaceBytesWithPaths.class,
                b.nana.technology.gingester.transformers.base.transformers.json.Set.class,
                b.nana.technology.gingester.transformers.base.transformers.json.Stream.class,
                b.nana.technology.gingester.transformers.base.transformers.json.ToString.class,
                b.nana.technology.gingester.transformers.base.transformers.json.Wrap.class,
                b.nana.technology.gingester.transformers.base.transformers.object.ToJson.class,
                b.nana.technology.gingester.transformers.base.transformers.path.Create.class,
                b.nana.technology.gingester.transformers.base.transformers.path.Delete.class,
                b.nana.technology.gingester.transformers.base.transformers.path.LastModified.class,
                b.nana.technology.gingester.transformers.base.transformers.path.Link.class,
                b.nana.technology.gingester.transformers.base.transformers.path.Move.class,
                b.nana.technology.gingester.transformers.base.transformers.path.Open.class,
                b.nana.technology.gingester.transformers.base.transformers.path.Search.class,
                b.nana.technology.gingester.transformers.base.transformers.path.Size.class,
                b.nana.technology.gingester.transformers.base.transformers.path.Write.class,
                b.nana.technology.gingester.transformers.base.transformers.regex.Filter.class,
                b.nana.technology.gingester.transformers.base.transformers.regex.Find.class,
                b.nana.technology.gingester.transformers.base.transformers.regex.Group.class,
                b.nana.technology.gingester.transformers.base.transformers.regex.Replace.class,
                b.nana.technology.gingester.transformers.base.transformers.regex.Route.class,
                b.nana.technology.gingester.transformers.base.transformers.resource.Open.class,
                b.nana.technology.gingester.transformers.base.transformers.std.In.class,
                b.nana.technology.gingester.transformers.base.transformers.std.Out.class,
                b.nana.technology.gingester.transformers.base.transformers.string.Append.class,
                b.nana.technology.gingester.transformers.base.transformers.string.AsBigDecimal.class,
                b.nana.technology.gingester.transformers.base.transformers.string.Create.class,
                b.nana.technology.gingester.transformers.base.transformers.string.ToBytes.class,
                b.nana.technology.gingester.transformers.base.transformers.string.ToInputStream.class,
                b.nana.technology.gingester.transformers.base.transformers.string.ToJson.class,
                b.nana.technology.gingester.transformers.base.transformers.string.Trim.class,
                b.nana.technology.gingester.transformers.base.transformers.time.AsOffsetDateTime.class,
                b.nana.technology.gingester.transformers.base.transformers.time.FromMillis.class,
                b.nana.technology.gingester.transformers.base.transformers.time.FromSeconds.class,
                b.nana.technology.gingester.transformers.base.transformers.time.FromString.class,
                b.nana.technology.gingester.transformers.base.transformers.time.Now.class,
                b.nana.technology.gingester.transformers.base.transformers.time.ToString.class,
                b.nana.technology.gingester.transformers.base.transformers.util.Cycle.class,
                b.nana.technology.gingester.transformers.base.transformers.util.Latch.class,
                b.nana.technology.gingester.transformers.base.transformers.util.Sample.class,
                b.nana.technology.gingester.transformers.base.transformers.util.Throttle.class,
                b.nana.technology.gingester.transformers.base.transformers.util.ToString.class,
                b.nana.technology.gingester.transformers.base.transformers.xml.ToJson.class
        );
    }
}

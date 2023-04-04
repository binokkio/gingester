package b.nana.technology.gingester.transformers.tika;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Names(1)
public class Tika implements Transformer<InputStream, InputStream> {

    private final org.apache.tika.Tika tika = new org.apache.tika.Tika();
    private final Template resourceNameKeyTemplate;
    private final Template contentTypeHintTemplate;

    public Tika(Parameters parameters) {

        resourceNameKeyTemplate = parameters.name == null ? null:
                Context.newTemplate(parameters.name);

        contentTypeHintTemplate = parameters.type == null ? null:
                Context.newTemplate(parameters.type);
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<InputStream> out) throws Exception {

        Metadata metadata = new Metadata();

        if (resourceNameKeyTemplate != null)
            metadata.add(TikaCoreProperties.RESOURCE_NAME_KEY, resourceNameKeyTemplate.render(context, in));

        if (contentTypeHintTemplate != null)
            metadata.add(TikaCoreProperties.CONTENT_TYPE_HINT, contentTypeHintTemplate.render(context, in));

        out.accept(
                context,
                new ReaderInputStream(tika.parse(in, metadata), StandardCharsets.UTF_8)
        );
    }

    public static class Parameters {
        public TemplateParameters name = new TemplateParameters("${description!''}");
        public TemplateParameters type;
    }
}

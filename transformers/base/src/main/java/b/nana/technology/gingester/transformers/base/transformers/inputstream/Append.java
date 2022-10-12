package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;

public final class Append implements Transformer<InputStream, InputStream> {

    private final TemplateMapper<byte[]> append;

    public Append(Parameters parameters) {
        append = Context.newTemplateMapper(parameters.append, s -> s.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<InputStream> out) {
        out.accept(context, new SequenceInputStream(in, new ByteArrayInputStream(append.render(context))));
    }

    public static class Parameters {

        public TemplateParameters append = new TemplateParameters("\n");

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(TemplateParameters append) {
            this.append = append;
        }
    }
}

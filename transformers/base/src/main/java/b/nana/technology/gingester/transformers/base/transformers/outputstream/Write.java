package b.nana.technology.gingester.transformers.base.transformers.outputstream;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Passthrough
@Names(1)
public final class Write implements Transformer<Object, Object> {

    private final FetchKey fetchTarget;
    private final Template messageTemplate;
    private final boolean newline;

    public Write(Parameters parameters) {
        fetchTarget = parameters.target;
        messageTemplate = Context.newTemplate(parameters.message);
        newline = parameters.newline;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        OutputStream outputStream = (OutputStream) context.require(fetchTarget);
        outputStream.write(messageTemplate.render(context, in).getBytes(StandardCharsets.UTF_8));
        if (newline) outputStream.write('\n');
        out.accept(context, in);
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order({ "target", "message" })
    public static class Parameters {
        public FetchKey target;
        public TemplateParameters message;
        public boolean newline = true;
    }
}

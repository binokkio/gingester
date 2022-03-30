package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class Create implements Transformer<Object, String> {

    private final Template template;

    public Create(Parameters parameters) {
        template = Context.newTemplate(parameters.template);
    }

    @Override
    public void transform(Context context, Object in, Receiver<String> out) throws InterruptedException {
        out.accept(context, template.render(context));
    }

    public static class Parameters {

        public TemplateParameters template;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(TemplateParameters template) {
            this.template = template;
        }
    }
}

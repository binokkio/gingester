package b.nana.technology.gingester.test.transformers;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class Generate implements Transformer<Object, String> {

    private final Template string;

    public Generate(Parameters parameters) {
        string = Context.newTemplate(parameters.string);
    }

    @Override
    public void transform(Context context, Object in, Receiver<String> out) {
        out.accept(context, string.render(context, in));
    }

    private static class Parameters {
        public TemplateParameters string;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(TemplateParameters string) {
            this.string = string;
        }
    }
}

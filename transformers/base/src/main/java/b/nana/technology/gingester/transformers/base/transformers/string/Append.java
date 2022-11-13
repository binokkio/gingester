package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import static java.util.Objects.requireNonNull;

public final class Append implements Transformer<String, String> {

    private final Template append;

    public Append(Parameters parameters) {
        append = Context.newTemplate(requireNonNull(parameters.append));
    }

    @Override
    public void transform(Context context, String in, Receiver<String> out) {
        out.accept(context, in + append.render(context, in));
    }

    public static class Parameters {

        public TemplateParameters append;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(TemplateParameters append) {
            this.append = append;
        }
    }
}

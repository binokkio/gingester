package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.regex.Pattern;

public final class Replace implements Transformer<String, String> {

    private final TemplateMapper<Pattern> patternTemplate;
    private final Template replacementTemplate;

    public Replace(Parameters parameters) {
        patternTemplate = Context.newTemplateMapper(parameters.regex, Pattern::compile);
        replacementTemplate = Context.newTemplate(parameters.replacement);
    }

    @Override
    public void transform(Context context, String in, Receiver<String> out) throws Exception {
        Pattern pattern = patternTemplate.render(context);
        String replacement = replacementTemplate.render(context);
        out.accept(context, pattern.matcher(in).replaceAll(replacement));
    }

    public static class Parameters {

        public TemplateParameters regex;
        public TemplateParameters replacement = new TemplateParameters("_", true);

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(@JsonProperty("regex") TemplateParameters regex) {
            this.regex = regex;
        }
    }
}

package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.regex.Pattern;

public final class Replace implements Transformer<String, String> {

    private final TemplateMapper<Pattern> patternTemplate;
    private final Template replacementTemplate;

    public Replace(Parameters parameters) {
        patternTemplate = Context.newTemplateMapper(parameters.regex, Pattern::compile);
        replacementTemplate = Context.newTemplate(parameters.replacement);
    }

    @Override
    public void transform(Context context, String in, Receiver<String> out) {
        Pattern pattern = patternTemplate.render(context);
        String replacement = replacementTemplate.render(context);
        out.accept(context, pattern.matcher(in).replaceAll(replacement));
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, regex -> o("regex", regex));
                rule(JsonNode::isArray, array -> o("regex", array.get(0), "replacement", array.get(1)));
            }
        }

        public TemplateParameters regex;
        public TemplateParameters replacement = new TemplateParameters("_", true);
    }
}

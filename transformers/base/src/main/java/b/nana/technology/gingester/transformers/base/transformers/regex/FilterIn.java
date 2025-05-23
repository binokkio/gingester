package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.annotations.Description;
import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.annotations.Passthrough;
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

@Description("Pass only items when the given regex matches")
@Example(example = "hello", description = "Pass only items containing \"hello\"")
@Example(example = "'hello|bye'", description = "Pass only items containing \"hello\" or \"bye\"")
@Example(example = "'^Hello, World!$'", description = "Pass only exactly \"Hello, World!\"")
@Example(example = "'Hello.*!'", description = "Pass only items containing \"Hello\" followed by \"!\" anywhere on the same line")
@Example(example = "'(?s)Hello.*!'", description = "Pass only items containing \"Hello\" followed by an \"!\" anywhere")
@Passthrough
public final class FilterIn implements Transformer<Object, Object> {

    private final TemplateMapper<Pattern> pattern;
    private final Template input;

    public FilterIn(Parameters parameters) {
        pattern = Context.newTemplateMapper(parameters.regex, Pattern::compile);

        if (parameters.input != null) {
            input = Context.newTemplate(parameters.input);
            if (input.isInvariant()) {
                throw new IllegalArgumentException("RegexFilterIn input template must not be invariant");
            }
        } else {
            input = null;
        }
    }

    @Override
    public Class<?> getInputType() {
        return input == null ? String.class : Object.class;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {
        String string = input == null ? (String) in : input.render(context, in);
        if (pattern.render(context, in).matcher(string).find()) {
            out.accept(context, in);
        }
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, regex -> o("regex", regex));
                rule(JsonNode::isArray, array -> o("input", array.get(0), "regex", array.get(1)));
            }
        }

        public TemplateParameters regex;
        public TemplateParameters input;
    }
}

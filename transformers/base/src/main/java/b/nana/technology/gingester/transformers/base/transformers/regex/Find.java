package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Find implements Transformer<String, Matcher> {

    private final TemplateMapper<Pattern> pattern;

    public Find(Parameters parameters) {
        pattern = Context.newTemplateMapper(parameters.regex, Pattern::compile);
    }

    @Override
    public void transform(Context context, String in, Receiver<Matcher> out) {
        Matcher matcher = pattern.render(context, in).matcher(in);
        while (matcher.find()) {
            out.accept(context, matcher);
        }
    }

    public static class Parameters {

        public TemplateParameters regex;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(TemplateParameters regex) {
            this.regex = regex;
        }
    }
}

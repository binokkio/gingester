package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.regex.Pattern;

public class Replace implements Transformer<String, String> {

    private final Pattern pattern;
    private final Context.Template replacementTemplate;

    public Replace(Parameters parameters) {
        pattern = Pattern.compile(parameters.pattern);
        replacementTemplate = Context.newTemplate(parameters.replacement);
    }

    @Override
    public void transform(Context context, String in, Receiver<String> out) throws Exception {
        out.accept(context, pattern.matcher(in).replaceAll(replacementTemplate.render(context)));
    }

    public static class Parameters {

        public String pattern;
        public String replacement = "_";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String pattern) {
            this.pattern = pattern;
        }
    }
}

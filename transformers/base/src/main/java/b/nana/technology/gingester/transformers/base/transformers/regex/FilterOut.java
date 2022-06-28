package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class FilterOut implements Transformer<String, String> {

    private final List<TemplateMapper<Pattern>> patterns;

    public FilterOut(Parameters parameters) {
        patterns = parameters.regexes.stream()
                .map(tp -> Context.newTemplateMapper(tp, Pattern::compile))
                .collect(Collectors.toList());
    }

    @Override
    public void transform(Context context, String in, Receiver<String> out) {
        if (patterns.stream().noneMatch(pattern -> pattern.render(context).matcher(in).find())) {
            out.accept(context, in);
        }
    }

    public static class Parameters {

        public List<TemplateParameters> regexes;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(List<TemplateParameters> regexes) {
            this.regexes = regexes;
        }
    }
}

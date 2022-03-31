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

public final class FilterIn implements Transformer<String, String> {

    private final List<TemplateMapper<Pattern>> patterns;

    public FilterIn(Parameters parameters) {
        patterns = parameters.patterns.stream()
                .map(tp -> Context.newTemplateMapper(tp, Pattern::compile))
                .collect(Collectors.toList());
    }

    @Override
    public void transform(Context context, String in, Receiver<String> out) throws Exception {
        if (patterns.stream().anyMatch(pattern -> pattern.render(context).matcher(in).matches())) {
            out.accept(context, in);
        }
    }

    public static class Parameters {

        public List<TemplateParameters> patterns;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(List<TemplateParameters> patterns) {
            this.patterns = patterns;
        }
    }
}

package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.annotations.Description;
import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Description("Pass only items of which a subsequence matches any of the given regexes")
@Example(example = "hello", description = "Pass only items containing \"hello\"")
@Example(example = "'[\"hello\", \"bye\"]", description = "Pass only items containing \"hello\" or \"bye\"")
@Example(example = "'^Hello, World!$'", description = "Pass only exactly \"Hello, World!\"")
@Example(example = "'Hello.*!'", description = "Pass only items containing \"Hello\" followed by an '!' anywhere on the same line")
@Example(example = "'(?s)Hello.*!'", description = "Pass only items containing \"Hello\" followed by an '!' anywhere")
public final class FilterIn implements Transformer<String, String> {

    private final List<TemplateMapper<Pattern>> patterns;

    public FilterIn(Parameters parameters) {
        patterns = parameters.regexes.stream()
                .map(tp -> Context.newTemplateMapper(tp, Pattern::compile))
                .collect(Collectors.toList());
    }

    @Override
    public void transform(Context context, String in, Receiver<String> out) throws Exception {
        if (patterns.stream().anyMatch(pattern -> pattern.render(context).matcher(in).find())) {
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

package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class FilterOut implements Transformer<String, String> {

    private final List<Pattern> patterns;

    public FilterOut(Parameters parameters) {
        patterns = parameters.patterns.stream().map(Pattern::compile).collect(Collectors.toList());
    }

    @Override
    public void transform(Context context, String in, Receiver<String> out) throws Exception {
        if (patterns.stream().noneMatch(pattern -> pattern.matcher(in).matches())) {
            out.accept(context, in);
        }
    }

    public static class Parameters {

        public List<String> patterns;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(List<String> patterns) {
            this.patterns = patterns;
        }
    }
}

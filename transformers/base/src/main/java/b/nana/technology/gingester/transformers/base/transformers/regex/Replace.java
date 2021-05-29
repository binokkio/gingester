package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.regex.Pattern;

public class Replace extends Transformer<String, String> {

    private final Pattern pattern;
    private final String replacement;

    public Replace(Parameters parameters) {
        pattern = Pattern.compile(parameters.pattern);
        replacement = parameters.replacement;
    }

    @Override
    protected void transform(Context context, String input) throws Exception {
        emit(context, pattern.matcher(input).replaceAll(replacement));
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

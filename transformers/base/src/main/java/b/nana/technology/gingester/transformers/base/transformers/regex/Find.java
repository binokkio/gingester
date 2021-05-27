package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Find extends Transformer<String, Matcher> {

    private final Pattern pattern;

    public Find() {
        this(new Parameters());
    }

    public Find(Parameters parameters) {
        super(parameters);
        this.pattern = Pattern.compile(parameters.pattern);
    }

    @Override
    protected void setup(Setup setup) {

    }

    @Override
    protected void transform(Context context, String input) throws Exception {
        Matcher matcher = pattern.matcher(input);
        if (!matcher.find()) throw new IllegalStateException("Pattern not found");
        emit(context, matcher);
    }

    public static class Parameters {

        public String pattern;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String pattern) {
            this.pattern = pattern;
        }
    }
}

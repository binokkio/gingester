package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Find implements Transformer<String, Matcher> {

    private final Pattern pattern;

    public Find() {
        this(new Parameters());
    }

    public Find(Parameters parameters) {
        this.pattern = Pattern.compile(parameters.pattern);
    }

    @Override
    public void transform(Context context, String in, Receiver<Matcher> out) throws Exception {
        Matcher matcher = pattern.matcher(in);
        if (!matcher.find()) throw new IllegalStateException("Pattern not found");
        out.accept(context, matcher);
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

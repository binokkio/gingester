package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.regex.Matcher;

public class Group extends Transformer<Matcher, String> {

    private final int group;

    public Group() {
        this(new Parameters());
    }

    public Group(Parameters parameters) {
        super(parameters);
        group = parameters.group;
    }

    @Override
    protected void transform(Context context, Matcher input) throws Exception {
        emit(context, input.group(group));
    }

    public static class Parameters {

        public int group;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(int group) {
            this.group = group;
        }
    }
}

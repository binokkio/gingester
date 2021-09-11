package b.nana.technology.gingester.transformers.base.transformers.regex;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.regex.Matcher;

public class Group implements Transformer<Matcher, String> {

    private final int group;

    public Group() {
        this(new Parameters());
    }

    public Group(Parameters parameters) {
        group = parameters.group;
    }

    @Override
    public void transform(Context context, Matcher in, Receiver<String> out) throws Exception {
        out.accept(context, in.group(group));
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

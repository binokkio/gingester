package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.ContextMap;
import b.nana.technology.gingester.core.Passthrough;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.HashSet;
import java.util.Set;

public class Concatenate extends Transformer<String, String> {

    private final ContextMap<StringBuilder> contextMap = new ContextMap<>();
    private final String delimiter;

    public Concatenate(Parameters parameters) {
        super(parameters);
        delimiter = parameters.delimiter;
    }

    @Override
    protected void prepare(Context context) {
        contextMap.put(context, new StringBuilder());
    }

    @Override
    protected void transform(Context context, String input) throws Exception {
        StringBuilder stringBuilder = contextMap.require(context);
        if (delimiter != null && stringBuilder.length() > 0) {
            stringBuilder.append(delimiter).append(input);
        } else {
            stringBuilder.append(input);
        }
    }

    @Override
    protected void finish(Context context) {
        emit(context, contextMap.requireRemove(context).toString());
    }

    public static class Parameters {

        public String delimiter;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String delimiter) {
            this.delimiter = delimiter;
        }
    }
}

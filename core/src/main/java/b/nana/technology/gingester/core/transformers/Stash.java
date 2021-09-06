package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public final class Stash implements Transformer<Object, Object> {

    private final String name;
    private final Object value;

    public Stash(Parameters parameters) {
        name = parameters.name;
        value = parameters.value;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        out.accept(context.stash(name, value == null ? in : value), in);
    }

    public static class Parameters {

        public String name = "stash";
        public Object value;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String name) {
            this.name = name;
        }
    }
}

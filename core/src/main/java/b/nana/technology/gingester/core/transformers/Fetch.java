package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;

public final class Fetch implements Transformer<Object, Object> {

    private final String name;
    private final String description;

    public Fetch(Parameters parameters) {
        name = parameters.name;
        description = String.format("Fetch \"%s\"", name);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        out.accept(
                context.stash(Map.of("description", description)),
                context.fetch(name).findFirst().orElseThrow()
        );
    }

    public static class Parameters {

        public String name = "stash";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String name) {
            this.name = name;
        }
    }
}

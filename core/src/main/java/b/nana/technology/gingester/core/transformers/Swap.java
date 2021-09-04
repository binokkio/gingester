package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;

public final class Swap implements Transformer<Object, Object> {

    private final String name;
    private final String description;

    public Swap(Parameters parameters) {
        name = parameters.name;
        description = String.format("Swap \"%s\"", name);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        out.accept(
                context.extend().stash(Map.of(
                        "description", description,
                        name, in
                )),
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

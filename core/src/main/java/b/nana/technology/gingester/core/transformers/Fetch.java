package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class Fetch implements Transformer<Object, Object> {

    private final String[] name;

    public Fetch(Parameters parameters) {
        name = parameters.name.split("/");  // TODO support escape sequence
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        out.accept(
                context,
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

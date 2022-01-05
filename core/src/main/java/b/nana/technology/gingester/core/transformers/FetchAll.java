package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

@Names(1)
public final class FetchAll implements Transformer<Object, Object> {  // TODO implement OutputFetcher and support @Pure bridges

    public static String[] parseStashName(String name) {
        return name.split("\\.");  // TODO support escape sequence
    }

    private final String[] name;

    public FetchAll(Parameters parameters) {
        name = parseStashName(parameters.stash);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        context.fetch(name).forEach(item -> out.accept(context, item));
    }

    public static class Parameters {

        public String stash = "stash";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String stash) {
            this.stash = stash;
        }
    }
}

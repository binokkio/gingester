package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.OutputFetcher;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.NoSuchElementException;

@Names(1)
public final class Fetch implements Transformer<Object, Object>, OutputFetcher {

    public static String[] parseStashName(String name) {
        if (name.isEmpty()) return new String[0];  // TODO maybe a specific type is better than a String[]
        else return name.split("\\.");  // TODO support escape sequence
    }

    private final String[] name;

    public Fetch(Parameters parameters) {
        name = parseStashName(parameters.stash);
    }

    @Override
    public String[] getOutputStashName() {
        return name;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        out.accept(
                context,
                context.fetch(name).findFirst().orElseThrow(() -> new NoSuchElementException("Nothing stashed as \"" + String.join(".", name) + "\""))
        );
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

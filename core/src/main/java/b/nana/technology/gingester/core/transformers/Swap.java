package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.InputStasher;
import b.nana.technology.gingester.core.transformer.OutputFetcher;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.NoSuchElementException;

@Names(1)
public final class Swap implements Transformer<Object, Object>, InputStasher, OutputFetcher {

    private final String name;

    public Swap(Parameters parameters) {
        name = parameters.name;
    }

    @Override
    public String getInputStashName() {
        return name;
    }

    @Override
    public String[] getOutputStashName() {
        return new String[] { name };
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        out.accept(
                context.stash(name, in),
                context.fetch(name).findFirst().orElseThrow(() -> new NoSuchElementException("Nothing stashed as \"" + String.join(".", name) + "\""))
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

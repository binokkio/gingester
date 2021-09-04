package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;

public class Generate implements Transformer<Object, String> {

    private final String string;
    private final int count;

    public Generate(Parameters parameters) {
        string = parameters.string;
        count = parameters.count;
    }

    @Override
    public void transform(Context context, Object in, Receiver<String> out) throws InterruptedException {
        for (int i = 0; i < count; i++) {
            if (Thread.interrupted()) throw new InterruptedException();
            out.accept(context.extend().stash(Map.of("description", i)), string);
        }
    }

    public static class Parameters {

        public String string;
        public int count = 1;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String string) {
            this.string = string;
        }
    }
}

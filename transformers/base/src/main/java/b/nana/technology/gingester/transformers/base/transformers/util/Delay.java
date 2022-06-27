package b.nana.technology.gingester.transformers.base.transformers.util;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

@Passthrough
public class Delay implements Transformer<Object, Object> {

    private final long millis;

    public Delay(Parameters parameters) {
        millis = parameters.millis;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        Thread.sleep(millis);
        out.accept(context, in);
    }

    public static class Parameters {

        public long millis;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(long millis) {
            this.millis = millis;
        }
    }
}

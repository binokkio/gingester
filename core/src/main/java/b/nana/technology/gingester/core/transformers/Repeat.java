package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.concurrent.atomic.AtomicInteger;

@Names(1)
@Passthrough
public final class Repeat implements Transformer<Object, Object> {

    private final int times;

    public Repeat(Parameters parameters) {
        times = parameters.times;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws InterruptedException {
        for (int i = 0; i < times; i++) {
            if (Thread.interrupted()) throw new InterruptedException();
            out.accept(context.stash("description", i), in);
        }
    }

    public static class Parameters {

        public int times = 2;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(int times) {
            this.times = times;
        }
    }
}

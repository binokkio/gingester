package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.concurrent.atomic.AtomicInteger;

public final class Monkey implements Transformer<Object, Object> {

    private final int interval;
    private final AtomicInteger counter = new AtomicInteger();

    public Monkey(Parameters parameters) {
        interval = parameters.interval;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {

        if (counter.incrementAndGet() % interval == 0) {
            throw new Bananas();
        }

        out.accept(context, in);
    }

    public static class Parameters {

        public int interval = 2;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(int interval) {
            this.interval = interval;
        }
    }

    public static class Bananas extends RuntimeException {

    }
}

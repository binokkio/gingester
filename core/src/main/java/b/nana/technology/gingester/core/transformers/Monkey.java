package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Description;
import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.concurrent.atomic.AtomicInteger;

@Names(1)
@Passthrough
@Description("Throw an exception for every nth item, pass the rest")
@Example(example = "3", description = "Throw an exception for the 3rd, 6th, 9th item and so forth, pass the rest")
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
        } else {
            out.accept(context, in);
        }
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
        private Bananas() {
            super("\uD83D\uDC35\uD83C\uDF4C");
        }
    }
}

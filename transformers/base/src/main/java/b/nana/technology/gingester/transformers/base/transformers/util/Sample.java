package b.nana.technology.gingester.transformers.base.transformers.util;

import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.concurrent.atomic.AtomicLong;

@Names(1)
@Passthrough
@Example(example = "100", description = "Drop all but the first of every 100 items")
public final class Sample implements Transformer<Object, Object> {

    private final ContextMap<AtomicLong> contextMap = new ContextMap<>();
    private final long divider;

    public Sample(Parameters parameters) {
        divider = parameters.divider;
    }

    @Override
    public void prepare(Context context, Receiver<Object> out) {
        contextMap.put(context, new AtomicLong());
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        if (contextMap.get(context).getAndIncrement() % divider == 0) {
            out.accept(context, in);
        }
    }

    @Override
    public void finish(Context context, Receiver<Object> out) {
        contextMap.remove(context);
    }

    public static class Parameters {

        public long divider = 10;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(long divider) {
            this.divider = divider;
        }
    }
}

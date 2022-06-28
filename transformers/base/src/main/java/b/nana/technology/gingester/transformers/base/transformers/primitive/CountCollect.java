package b.nana.technology.gingester.transformers.base.transformers.primitive;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

@Names(1)
public final class CountCollect implements Transformer<Object, Long> {

    private final ContextMap<long[]> counters = new ContextMap<>();

    @Override
    public void prepare(Context context, Receiver<Long> out) {
        counters.put(context, new long[1]);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Long> out) throws Exception {
        counters.act(context, counter -> counter[0]++);
    }

    @Override
    public void finish(Context context, Receiver<Long> out) {
        out.accept(context, counters.remove(context)[0]);
    }
}

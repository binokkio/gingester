package b.nana.technology.gingester.test.transformers;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.concurrent.atomic.AtomicInteger;

@Names(1)
public final class SyncCounter implements Transformer<Object, Integer> {

    private final ContextMap<AtomicInteger> state = new ContextMap<>();

    @Override
    public void prepare(Context context, Receiver<Integer> out) {
        state.put(context, new AtomicInteger());
    }

    @Override
    public void transform(Context context, Object in, Receiver<Integer> out) throws Exception {
        state.apply(context, AtomicInteger::incrementAndGet);
    }

    @Override
    public void finish(Context context, Receiver<Integer> out) {
        out.accept(context, state.remove(context).get());
    }
}

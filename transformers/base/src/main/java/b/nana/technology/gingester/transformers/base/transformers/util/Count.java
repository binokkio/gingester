package b.nana.technology.gingester.transformers.base.transformers.util;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.concurrent.atomic.AtomicLong;

@Names(1)
@Passthrough
public final class Count implements Transformer<Object, Object> {

    private final ContextMap<AtomicLong> contextMap = new ContextMap<>();

    @Override
    public void prepare(Context context, Receiver<Object> out) {
        contextMap.put(context, new AtomicLong());
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {
        out.accept(context.stash("count", contextMap.get(context).getAndIncrement()), in);
    }

    @Override
    public void finish(Context context, Receiver<Object> out) {
        contextMap.remove(context);
    }
}

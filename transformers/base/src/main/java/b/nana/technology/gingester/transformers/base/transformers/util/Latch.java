package b.nana.technology.gingester.transformers.base.transformers.util;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

@Names(1)
@Passthrough
public final class Latch implements Transformer<Object, Object> {

    private final ContextMap<AtomicReference<Object>> state = new ContextMap<>();

    @Override
    public void setup(SetupControls controls) {
        controls.syncs(Collections.singletonList("__seed__"));
    }

    @Override
    public void prepare(Context context, Receiver<Object> out) {
        state.put(context, new AtomicReference<>());
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        state.act(context, latch -> {
            if (!in.equals(latch.get())) {
                latch.set(in);
                out.accept(context, in);
            }
        });
    }

    @Override
    public void finish(Context context, Receiver<Object> out) {
        state.remove(context);
    }
}

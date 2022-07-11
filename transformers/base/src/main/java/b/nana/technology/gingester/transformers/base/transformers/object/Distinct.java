package b.nana.technology.gingester.transformers.base.transformers.object;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.HashSet;
import java.util.Set;

@Names(1)
@Passthrough
public final class Distinct implements Transformer<Object, Object> {

    private final ContextMap<Set<Object>> states = new ContextMap<>();

    @Override
    public void prepare(Context context, Receiver<Object> out) throws Exception {
        states.put(context, new HashSet<>());
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        boolean distinct = states.apply(context, set -> set.add(in));
        if (distinct) out.accept(context, in);
    }

    @Override
    public void finish(Context context, Receiver<Object> out) throws Exception {
        states.remove(context);
    }
}

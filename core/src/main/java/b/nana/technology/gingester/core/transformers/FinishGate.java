package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.ArrayList;
import java.util.List;

@Passthrough
@Names(1)
public final class FinishGate implements Transformer<Object, Object> {

    private final ContextMap<List<Object>> contextMap = new ContextMap<>();

    @Override
    public void prepare(Context context, Receiver<Object> out) throws Exception {
        contextMap.put(context, new ArrayList<>());
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        contextMap.act(context, objects -> objects.add(in));
    }

    @Override
    public void finish(Context context, Receiver<Object> out) throws Exception {
        for (Object object : contextMap.remove(context)) {
            out.accept(context, object);
        }
    }
}

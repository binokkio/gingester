package b.nana.technology.gingester.transformers.base.transformers.util;

import b.nana.technology.gingester.core.annotations.Experimental;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.Map;

@Experimental
@Names(1)
public final class Pair implements Transformer<Object, Object> {

    private final ContextMap<Object[]> previous = new ContextMap<>();

    @Override
    public void prepare(Context context, Receiver<Object> out) throws Exception {
        previous.put(context, new Object[1]);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {

        Map<String, Object> output = previous.apply(context, holder -> {

            if (holder[0] == null) {
                holder[0] = in;
                return null;
            }

            Map<String, Object> o = Map.of(
                    "a", holder[0],
                    "b", in
            );

            holder[0] = in;

            return o;
        });

        if (output != null)
            out.accept(context.stash(output), output);
    }

    @Override
    public void finish(Context context, Receiver<Object> out) throws Exception {
        previous.remove(context);
    }
}

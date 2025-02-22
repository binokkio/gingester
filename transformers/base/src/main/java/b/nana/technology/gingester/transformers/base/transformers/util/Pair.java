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

    private final ContextMap<GroupContextAndPreviousValue> previous = new ContextMap<>();

    @Override
    public void prepare(Context context, Receiver<Object> out) throws Exception {
        previous.put(context, new GroupContextAndPreviousValue(context));
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {

        Map<String, Object> output = previous.apply(context, holder -> {

            if (holder.previousValue == null) {
                holder.previousValue = in;
                return null;
            }

            Map<String, Object> o = Map.of(
                    "a", holder.previousValue,
                    "b", in
            );

            holder.previousValue = in;

            return o;
        });

        if (output != null)
            out.accept(context.stash(output), output);  // TODO maybe use group context?
    }

    @Override
    public void finish(Context context, Receiver<Object> out) throws Exception {
        previous.remove(context);
    }

    private static class GroupContextAndPreviousValue {
        private final Context groupContext;
        private Object previousValue;

        private GroupContextAndPreviousValue(Context groupContext) {
            this.groupContext = groupContext;
        }
    }
}

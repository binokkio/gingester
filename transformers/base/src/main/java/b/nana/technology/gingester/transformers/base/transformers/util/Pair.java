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

        GroupContextAndPair groupContextAndPair = previous.apply(context, holder -> {

            if (holder.previousValue == null) {
                holder.previousValue = in;
                return null;
            }

            Map<String, Object> pair = Map.of(
                    "a", holder.previousValue,
                    "b", in
            );

            holder.previousValue = in;

            return new GroupContextAndPair(holder.groupContext, pair);
        });

        if (groupContextAndPair != null)
            out.accept(groupContextAndPair.groupContext.stash(groupContextAndPair.pair), groupContextAndPair.pair);
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

    private static class GroupContextAndPair {
        private final Context groupContext;
        private final Map<String, Object> pair;

        private GroupContextAndPair(Context groupContext, Map<String, Object> pair) {
            this.groupContext = groupContext;
            this.pair = pair;
        }
    }
}

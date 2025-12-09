package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

// TODO move some stuff to the base package, only things with dedicated GCLI syntax should remain here

@Names(1)
@Passthrough
@Example(example = "3", description = "Skip the first 3 items and yield the rest")
public final class Skip implements Transformer<Object, Object> {

    private final ContextMap<int[]> state = new ContextMap<>();

    private final int skip;

    public Skip(Parameters parameters) {
        skip = parameters.skip;
    }

    @Override
    public void prepare(Context context, Receiver<Object> out) throws Exception {
        state.put(context, new int[1]);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {

        boolean yield = state.apply(context, holder -> {
            if (holder[0] <= skip) {
                holder[0]++;
                return false;
            } else {
                return true;
            }
        });

        if (yield)
            out.accept(context, in);
    }

    @Override
    public void finish(Context context, Receiver<Object> out) throws Exception {
        state.remove(context);
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order("skip")
    public static class Parameters {
        public int skip;
    }
}

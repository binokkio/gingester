package b.nana.technology.gingester.transformers.base.transformers.list;

import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

@Example(example = "0", description = "Get the first item in the list")
@Example(example = "@ -1", description = "Get the last item in the list")
public final class Get implements Transformer<List<?>, Object> {

    private final int index;

    public Get(Parameters parameters) {
        index = parameters.index;
    }

    @Override
    public void transform(Context context, List<?> in, Receiver<Object> out) throws Exception {
        if (index < 0) out.accept(context.stash("description", index), in.get(in.size() + index));  // TODO test
        else out.accept(context.stash("description", index), in.get(index));
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order("index")
    public static class Parameters {
        public int index = 0;
    }
}

package b.nana.technology.gingester.transformers.base.transformers.list;

import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

public final class Collect implements Transformer<Object, List<?>> {

    private final ContextMap<List<Object>> contextMap = new ContextMap<>();
    private final ListType listType;

    public Collect(Collect.Parameters parameters) {
        listType = parameters.type;
    }

    @Override
    public void prepare(Context context, Receiver<List<?>> out) {
        contextMap.put(context, listType.newList());
    }

    @Override
    public void transform(Context context, Object in, Receiver<List<?>> out) throws Exception {
        contextMap.act(context, list -> list.add(in));
    }

    @Override
    public void finish(Context context, Receiver<List<?>> out) {
        out.accept(context, contextMap.remove(context));
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order("type")
    public static class Parameters {
        public ListType type = ListType.ARRAY_LIST;
    }
}

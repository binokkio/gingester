package b.nana.technology.gingester.transformers.base.transformers.list;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public final class Collect implements Transformer<Object, List<?>> {

    private final ContextMap<List<Object>> contextMap = new ContextMap<>();
    private final Supplier<List<Object>> listSupplier;

    public Collect(Collect.Parameters parameters) {
        listSupplier = parameters.type.listSupplier;
    }

    @Override
    public void prepare(Context context, Receiver<List<?>> out) throws Exception {
        contextMap.put(context, listSupplier.get());
    }

    @Override
    public void transform(Context context, Object in, Receiver<List<?>> out) throws Exception {
        contextMap.act(context, list -> list.add(in));
    }

    @Override
    public void finish(Context context, Receiver<List<?>> out) throws Exception {
        out.accept(context, contextMap.remove(context));
    }

    public static class Parameters {

        public Type type = Type.ARRAY_LIST;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(Type type) {
            this.type = type;
        }

        public enum Type {

            @JsonProperty("hash")
            ARRAY_LIST(ArrayList::new),

            @JsonProperty("linked")
            LINKED_LIST(LinkedList::new);

            private final Supplier<List<Object>> listSupplier;

            Type(Supplier<List<Object>> listSupplier) {
                this.listSupplier = listSupplier;
            }
        }
    }
}

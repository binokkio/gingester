package b.nana.technology.gingester.transformers.base.transformers.map;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMapReduce;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformers.Fetch;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class CollectCollections<T extends Collection<?>> implements Transformer<Object, Map<Object, T>> {

    private final ContextMapReduce<Map<Object, T>> maps = new ContextMapReduce<>();

    private final String[] valueStashName;

    public CollectCollections(Parameters parameters) {
        valueStashName = Fetch.parseStashName(parameters.value);
    }

    protected abstract T supply();
    protected abstract void add(T collection, Object value);
    protected abstract T reduce(T a, T b);

    @Override
    public void prepare(Context context, Receiver<Map<Object, T>> out) {
        maps.put(context, HashMap::new);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Map<Object, T>> out) throws Exception {
        Optional<Object> value = context.fetch(valueStashName).findFirst();
        if (value.isPresent()) {
            add(
                    maps.get(context).computeIfAbsent(in, x -> supply()),
                    value.get()
            );
        } else {
            throw new NoSuchElementException("Empty value fetch: " + String.join(".", valueStashName));
        }
    }

    @Override
    public void finish(Context context, Receiver<Map<Object, T>> out) {
        out.accept(context, maps.remove(context)
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        this::reduce,
                        HashMap::new
                )));
    }

    public static class Parameters {

        public String value = "";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String value) {
            this.value = value;
        }
    }
}

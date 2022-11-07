package b.nana.technology.gingester.transformers.base.transformers.map;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMapReduce;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class CollectCounts implements Transformer<Object, Map<Object, Long>> {

    private final ContextMapReduce<Map<Object, long[]>> maps = new ContextMapReduce<>();

    @Override
    public void prepare(Context context, Receiver<Map<Object, Long>> out) {
        maps.put(context, HashMap::new);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Map<Object, Long>> out) {
        maps.get(context).computeIfAbsent(in, x -> new long[1])[0]++;
    }

    @Override
    public void finish(Context context, Receiver<Map<Object, Long>> out) {
        out.accept(context, maps.remove(context)
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        counter -> counter.getValue()[0],
                        Long::sum,
                        HashMap::new
                )));
    }
}

package b.nana.technology.gingester.transformers.base.transformers.join;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import org.antlr.v4.runtime.misc.Pair;

import java.util.*;

@Names(1)
public final class OuterJoin implements Transformer<Object, Map<String, Object>> {

    private static final FetchKey FETCH_JOIN_AS = new FetchKey("joinAs");
    private static final FetchKey FETCH_VALUE = new FetchKey(1);

    private final ContextMap<Map<String, Map<Object, List<Object>>>> states = new ContextMap<>();

    private final Set<String> lists;

    public OuterJoin(Parameters parameters) {
        lists = new HashSet<>(parameters);
    }

    @Override
    public void prepare(Context context, Receiver<Map<String, Object>> out) throws Exception {
        states.put(context, new HashMap<>());
    }

    @Override
    public void transform(Context context, Object key, Receiver<Map<String, Object>> out) throws Exception {

        String joinAs = (String) context.require(FETCH_JOIN_AS);
        Object value = context.require(FETCH_VALUE);

        states.act(context, state -> state
                .computeIfAbsent(joinAs, x -> new HashMap<>())
                .computeIfAbsent(key, x -> new ArrayList<>())
                .add(value));
    }

    @Override
    public void finish(Context context, Receiver<Map<String, Object>> out) throws Exception {

        Map<String, Map<Object, List<Object>>> state = states.remove(context);
        Set<Object> seen = new HashSet<>();

        state.values().stream().map(Map::keySet).flatMap(Collection::stream).forEach(key -> {
            if (!seen.contains(key)) {
                seen.add(key);

                List<Pair<String, List<Object>>> data = new ArrayList<>();
                state.forEach((joinAs, joinData) ->
                        data.add(new Pair<>(joinAs, joinData.getOrDefault(key,
                                lists.contains(joinAs) ?
                                        Collections.emptyList() :
                                        Collections.singletonList(null)))));

                permutate(data, 0, new HashMap<>(), context, out);
            }
        });
    }

    private void permutate(List<Pair<String, List<Object>>> data, int depth, Map<String, Object> result, Context context, Receiver<Map<String, Object>> out) {

        boolean atMaxDepth = depth == data.size() - 1;

        Pair<String, List<Object>> pair = data.get(depth);
        String joinAs = pair.a;
        List<Object> values = pair.b;

        if (lists.contains(joinAs)) {
            result.put(joinAs, pair.b);
            if (atMaxDepth) {
                out.accept(context, new HashMap<>(result));
            } else {
                permutate(data, depth + 1, result, context, out);
            }
        } else {
            for (Object value : values) {
                result.put(joinAs, value);
                if (atMaxDepth) {
                    out.accept(context, new HashMap<>(result));
                } else {
                    permutate(data, depth + 1, result, context, out);
                }
            }
        }
    }

    public static class Parameters extends ArrayList<String> {

    }
}

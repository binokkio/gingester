package b.nana.technology.gingester.transformers.base.transformers.groupby;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.InputStasher;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.HashMap;
import java.util.Map;

@Passthrough
public final class Equals implements Transformer<Object, Object>, InputStasher {

    private final ContextMap<State> contextMap = new ContextMap<>();
    private final String stash;
    private final int limit;

    public Equals(Parameters parameters) {
        stash = parameters.stash;
        limit = parameters.limit;
    }

    @Override
    public String getInputStashName() {
        return stash;
    }

    @Override
    public void prepare(Context context, Receiver<Object> out) {
        contextMap.put(context, new State(context, out));
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        contextMap.act(context, state -> {
            Group group = state.getGroup(in);
            out.accept(context.group(group.context), in);
            state.count(in, group);
        });
    }

    @Override
    public void finish(Context context, Receiver<Object> out) {
        contextMap.remove(context).closeGroups();
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, stash -> o("stash", stash));
                rule(JsonNode::isInt, limit -> o("limit", limit));
                rule(JsonNode::isArray, array -> array.get(0).isTextual() ?
                        o("stash", array.get(0), "limit", array.get(1)) :
                        o("limit", array.get(0), "stash", array.get(1)));
            }
        }

        public String stash = "groupKey";
        public int limit = -1;
    }

    private class State {

        private final Context groupParent;
        private final Receiver<Object> out;
        private final Map<Object, Group> groups = new HashMap<>();

        public State(Context groupParent, Receiver<Object> out) {
            this.groupParent = groupParent;
            this.out = out;
        }

        private Group getGroup(Object key) {
            return groups.computeIfAbsent(key,
                    k -> new Group(out.acceptGroup(groupParent.stash(stash, k))));
        }

        private void count(Object key, Group group) {
            if (limit != -1 && ++group.counter == limit) {
                out.closeGroup(group.context);
                groups.remove(key);
            }
        }

        private void closeGroups() {
            for (Group group : groups.values()) {
                out.closeGroup(group.context);
            }
        }
    }

    private static class Group {
        private final Context context;
        private int counter;

        private Group(Context context) {
            this.context = context;
        }
    }
}

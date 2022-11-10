package b.nana.technology.gingester.transformers.base.transformers.groupby;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.common.LruMap;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.InputStasher;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

@Passthrough
public final class Equals implements Transformer<Object, Object>, InputStasher {

    private final ContextMap<State> contextMap = new ContextMap<>();
    private final String stash;
    private final int maxGroups;
    private final int maxEntries;

    public Equals(Parameters parameters) {
        stash = parameters.stash;
        maxGroups = parameters.maxGroups;
        maxEntries = parameters.maxEntries;
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

    private class State {

        private final Context groupParent;
        private final Receiver<Object> out;
        private final Map<Object, Group> groups;

        public State(Context groupParent, Receiver<Object> out) {
            this.groupParent = groupParent;
            this.out = out;
            this.groups = maxGroups != -1 ?
                    new LruMap<>(maxGroups, this::onExpelLru) :
                    new HashMap<>();
        }

        private Group getGroup(Object key) {
            return groups.computeIfAbsent(key,
                    k -> new Group(out.acceptGroup(groupParent.stash(stash, k))));
        }

        private void count(Object key, Group group) {
            if (maxEntries != -1 && ++group.counter == maxEntries) {
                out.closeGroup(group.context);
                groups.remove(key);
            }
        }

        private void onExpelLru(Object key, Group group) {
            out.closeGroup(group.context);
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

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, stash -> o("stash", stash));
                rule(JsonNode::isInt, maxEntries -> o("maxEntries", maxEntries));
                rule(JsonNode::isArray, array -> {
                    ObjectNode objectNode = o();
                    for (JsonNode element : array) {
                        if (element.isTextual() && !objectNode.has("stash")) {
                            objectNode.set("stash", element);
                        } else if (element.isInt()) {
                            if (!objectNode.has("maxEntries")) {
                                objectNode.set("stash", element);
                            } else if (!objectNode.has("maxGroups")) {
                                objectNode.set("stash", element);
                            } else {
                                throw new IllegalArgumentException("GroupByEquals parameter parsing failed at " + element);
                            }
                        } else {
                            throw new IllegalArgumentException("GroupByEquals parameter parsing failed at " + element);
                        }
                    }
                    return objectNode;
                });
            }
        }

        public String stash = "groupKey";
        public int maxGroups = -1;
        public int maxEntries = -1;
    }
}

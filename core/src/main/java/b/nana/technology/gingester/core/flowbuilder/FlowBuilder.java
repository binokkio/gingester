package b.nana.technology.gingester.core.flowbuilder;

import b.nana.technology.gingester.core.GingesterNext;
import b.nana.technology.gingester.core.transformer.TransformerFactory;
import b.nana.technology.gingester.core.transformers.ELog;
import b.nana.technology.gingester.core.transformers.passthrough.Passthrough;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FlowBuilder {

    private final Map<String, Node> nodes = new LinkedHashMap<>();

    private Node last;
    private List<String> linkFrom = List.of();
    private List<String> syncFrom = List.of();
    private List<String> divertFrom = List.of();

    public FlowBuilder() {

        Node elog = new Node();
        elog.id("__elog__");
        elog.transformer(new ELog());
        nodes.put(elog.requireId(), elog);

        Node seed = new Node();
        seed.id("__seed__");
        seed.transformer(new Passthrough());
        seed.addExcept("__elog__");
        nodes.put(seed.requireId(), seed);
    }

    public FlowBuilder add(Node node) {

        String id = getId(node);
        node.id(id);
        nodes.put(id, node);
        last = node;

        linkFrom.stream().map(nodes::get).forEach(n -> n.addLink(id));
        linkFrom = List.of(id);

        // TODO handle divert from
        divertFrom = List.of();

        return this;
    }

    public GingesterNext build() {
        return new GingesterNext(this);
    }

    public Map<String, Node> getNodes() {
        return nodes;
    }

    public String getLastId() {
        return last.requireId();
    }

    private String getId(Node node) {
        if (node.getId().isPresent()) {
            String id = node.getId().get();
            if (nodes.containsKey(id))
                throw new IllegalArgumentException("Transformer id " + id + " already in use");
            return id;
        } else {
            String name = node.getName()
                    .orElseGet(() -> TransformerFactory.getUniqueName(node.getTransformer().orElseThrow()));
            String id = name;
            int i = 1;
            while (nodes.containsKey(id))
                id = name + '_' + i++;
            return id;
        }
    }
}

package b.nana.technology.gingester.core.jsongraph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonGraph {

    public Map<String, Node> nodes = new LinkedHashMap<>();
    public List<Edge> edges = new ArrayList<>();

    public void add(String id, String transformer, Object parameters, List<String> links, List<String> syncs) {
        nodes.put(id, new Node(id, transformer, parameters));
        links.forEach(linkId -> edges.add(new Edge(id, linkId, "link")));
        syncs.forEach(linkId -> edges.add(new Edge(linkId, id, "sync")));
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(Map.of("graph", this));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Node {

        public String label;
        public Metadata metadata;

        Node(String id, String transformer, Object parameters) {
            label = id.replaceAll("_.*", "");
            label = label.equals(transformer) ? label : label + ":" + transformer;
            metadata = new Metadata(transformer, parameters);
        }

        public static class Metadata {
            public String transformer;
            public Object parameters;

            Metadata(String transformer, Object parameters) {
                this.transformer = transformer;
                this.parameters = parameters;
            }
        }
    }

    public static class Edge {

        public String source;
        public String target;
        public String relation;

        Edge(String source, String target, String relation) {
            this.source = source;
            this.target = target;
            this.relation = relation;
        }
    }
}

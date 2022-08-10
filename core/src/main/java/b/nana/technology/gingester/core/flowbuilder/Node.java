package b.nana.technology.gingester.core.flowbuilder;

import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Node {

    private final Map<String, String> links = new LinkedHashMap<>();
    private final Map<String, String> syncs = new LinkedHashMap<>();
    private final Map<String, String> excepts = new LinkedHashMap<>();

    private String id;
    private String name;
    private Transformer<?, ?> transformer;
    private SetupControls setupControls;
    private Integer maxWorkers;
    private Integer maxQueueSize;
    private Integer maxBatchSize;
    private Boolean report;

    public Node id(String id) {
        this.id = id;
        return this;
    }

    public Node name(String name) {
        this.name = name;
        return this;
    }

    public Node transformer(Transformer<?, ?> transformer) {
        this.transformer = transformer;
        this.setupControls = new SetupControls(transformer);
        transformer.setup(setupControls);
        setupControls.getLinks().ifPresent(list -> list.forEach(link -> links.put(link, link)));
        setupControls.getSyncs().ifPresent(list -> list.forEach(sync -> syncs.put(sync, sync)));
        setupControls.getExcepts().ifPresent(list -> list.forEach(except -> excepts.put(except, except)));
        return this;
    }

    public Node maxWorkers(int maxWorkers) {
        this.maxWorkers = maxWorkers;
        return this;
    }

    public Node maxQueueSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
        return this;
    }

    public Node maxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
        return this;
    }

    public Node report(boolean report) {
        this.report = report;
        return this;
    }



    public Node addLink(String link) {
        links.put(link, link);
        return this;
    }

    public Node addLinks(List<String> links) {
        links.forEach(link -> this.links.put(link, link));
        return this;
    }

    public Node addExcept(String except) {
        excepts.put(except, except);
        return this;
    }



    public Optional<String> getId() {
        return Optional.ofNullable(id);
    }

    public String requireId() {
        return getId().orElseThrow();
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Optional<Transformer<?, ?>> getTransformer() {
        return Optional.ofNullable(transformer);
    }

    public Transformer<?, ?> requireTransformer() {
        return getTransformer().orElseThrow();
    }

    public SetupControls getSetupControls() {
        return setupControls;
    }

    public Map<String, String> getLinks() {
        return links;
    }

    public Map<String, String> getSyncs() {
        return syncs;
    }

    public Map<String, String> getExcepts() {
        return excepts;
    }
}

package b.nana.technology.gingester.core;

import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.*;

public final class Node {

    private final Map<String, String> links = new LinkedHashMap<>();
    private final List<String> syncs = new ArrayList<>();
    private final List<String> excepts = new ArrayList<>();

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
        setupControls.getSyncs().ifPresent(syncs::addAll);
        setupControls.getExcepts().ifPresent(excepts::addAll);
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



    public Node setLinks(List<String> links) {
        this.links.clear();
        addLinks(links);
        return this;
    }

    public Node addLink(String link) {
        links.put(link, link);
        return this;
    }

    public Node addLink(String linkName, String link) {
        links.put(linkName, link);
        return this;
    }

    public Node addLinks(List<String> links) {
        links.forEach(link -> this.links.put(link, link));
        return this;
    }

    public String getLink(String linkName) {
        return links.get(linkName);
    }

    public Node updateLink(String linkName, String link) {
        links.put(linkName, link);
        return this;
    }



    public Node setSyncs(List<String> syncs) {
        this.syncs.clear();
        addSyncs(syncs);
        return this;
    }

    public Node addSync(String sync) {
        syncs.add(sync);
        return this;
    }

    public Node addSyncs(List<String> syncs) {
        this.syncs.addAll(syncs);
        return this;
    }



    public Node setExcepts(List<String> excepts) {
        this.excepts.clear();
        addExcepts(excepts);
        return this;
    }

    public Node addExcept(String except) {
        excepts.add(except);
        return this;
    }

    public Node addExcepts(List<String> excepts) {
        this.excepts.addAll(excepts);
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

    public List<String> getSyncs() {
        return syncs;
    }

    public List<String> getExcepts() {
        return excepts;
    }
}

package b.nana.technology.gingester.core.flowbuilder;

import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.LinkedHashMap;
import java.util.Map;

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
}

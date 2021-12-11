package b.nana.technology.gingester.core.configuration;

import b.nana.technology.gingester.core.reporting.Counter;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class ControllerConfiguration<I, O> {

    private String id;
    private Transformer<I, O> transformer;
    private Integer maxWorkers;
    private Integer maxBatchSize;
    private Integer maxQueueSize;
    private List<String> links = Collections.emptyList();
    private List<String> syncs = Collections.emptyList();
    private List<String> excepts = Collections.emptyList();
    private boolean report;
    private Counter acksCounter;



    public ControllerConfiguration<I, O> id(String id) {
        this.id = id;
        return this;
    }

    public ControllerConfiguration<I, O> transformer(Transformer<I, O> transformer) {
        this.transformer = transformer;
        return this;
    }

    public ControllerConfiguration<I, O> maxWorkers(int maxWorkers) {
        this.maxWorkers = maxWorkers;
        return this;
    }

    public ControllerConfiguration<I, O> maxQueueSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
        return this;
    }

    public ControllerConfiguration<I, O> maxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
        return this;
    }

    public ControllerConfiguration<I, O> links(List<String> links) {
        this.links = links;
        return this;
    }

    public ControllerConfiguration<I, O> syncs(List<String> syncs) {
        this.syncs = syncs;
        return this;
    }

    public ControllerConfiguration<I, O> excepts(List<String> excepts) {
        this.excepts = excepts;
        return this;
    }

    public ControllerConfiguration<I, O> report(boolean report) {
        this.report = report;
        return this;
    }

    public ControllerConfiguration<I, O> acksCounter(Counter acksCounter) {
        this.acksCounter = acksCounter;
        return this;
    }



    public String getId() {
        return id;
    }

    public Transformer<I, O> getTransformer() {
        return transformer;
    }

    public Optional<Integer> getMaxWorkers() {
        return Optional.ofNullable(maxWorkers);
    }

    public Optional<Integer> getMaxQueueSize() {
        return Optional.ofNullable(maxQueueSize);
    }

    public Optional<Integer> getMaxBatchSize() {
        return Optional.ofNullable(maxBatchSize);
    }

    public List<String> getLinks() {
        return links;
    }

    public List<String> getSyncs() {
        return syncs;
    }

    public List<String> getExcepts() {
        return excepts;
    }

    public boolean getReport() {
        return report;
    }

    public Counter getAcksCounter() {
        return acksCounter;
    }
}

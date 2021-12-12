package b.nana.technology.gingester.core.configuration;

import java.util.List;
import java.util.Optional;

public abstract class BaseConfiguration<T extends BaseConfiguration<T>> {

    private Integer maxWorkers;
    private Integer maxQueueSize;
    private Integer maxBatchSize;
    private List<String> links;
    private List<String> syncs;
    private List<String> excepts;



    public T maxWorkers(Integer maxWorkers) {
        this.maxWorkers = maxWorkers;
        return (T) this;
    }

    public T maxQueueSize(Integer maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
        return (T) this;
    }

    public T maxBatchSize(Integer maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
        return (T) this;
    }

    public T links(List<String> links) {
        this.links = links;
        return (T) this;
    }

    public T syncs(List<String> syncs) {
        this.syncs = syncs;
        return (T) this;
    }

    public T excepts(List<String> excepts) {
        this.excepts = excepts;
        return (T) this;
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

    public Optional<List<String>> getLinks() {
        return Optional.ofNullable(links);
    }

    public Optional<List<String>> getSyncs() {
        return Optional.ofNullable(syncs);
    }

    public Optional<List<String>> getExcepts() {
        return Optional.ofNullable(excepts);
    }
}

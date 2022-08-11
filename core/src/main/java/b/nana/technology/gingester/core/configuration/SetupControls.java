package b.nana.technology.gingester.core.configuration;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.reporting.Counter;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public final class SetupControls {

    private Integer maxWorkers;
    private Integer maxQueueSize;
    private Integer maxBatchSize;
    private List<String> links;
    private List<String> syncs;
    private List<String> excepts;
    private boolean requireOutgoingSync;
    private boolean requireOutgoingAsync;
    private Counter acksCounter;

    public SetupControls(Transformer<?, ?> transformer) {

        // if prepare or finish are overridden then preconfigure this SetupControls to sync with __seed__
        try {

            Method prepare = transformer.getClass().getMethod("prepare", Context.class, Receiver.class);
            Method finish = transformer.getClass().getMethod("finish", Context.class, Receiver.class);

            if (isOverridden(prepare) || isOverridden(finish)) {
                syncs(List.of("__seed__"));
            }

        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean isOverridden(Method method) {
        return !method.getDeclaringClass().equals(Transformer.class);
    }



    public SetupControls maxWorkers(Integer maxWorkers) {
        this.maxWorkers = maxWorkers;
        return this;
    }

    public SetupControls maxQueueSize(Integer maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
        return this;
    }

    public SetupControls maxBatchSize(Integer maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
        return this;
    }

    public SetupControls links(List<String> links) {
        this.links = links;
        return this;
    }

    public SetupControls syncs(List<String> syncs) {
        this.syncs = syncs;
        return this;
    }

    public SetupControls excepts(List<String> excepts) {
        this.excepts = excepts;
        return this;
    }

    public SetupControls requireOutgoingSync() {
        this.requireOutgoingSync = true;
        return this;
    }

    public SetupControls requireOutgoingAsync() {
        this.requireOutgoingAsync = true;
        return this;
    }

    // TODO requireOutgoingMaxBatchSize?

    public SetupControls acksCounter(Counter acksCounter) {
        this.acksCounter = acksCounter;
        return this;
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

    public boolean getRequireOutgoingSync() {
        return requireOutgoingSync;
    }

    public boolean getRequireOutgoingAsync() {
        return requireOutgoingAsync;
    }

    public Optional<Counter> getAcksCounter() {
        return Optional.ofNullable(acksCounter);
    }
}

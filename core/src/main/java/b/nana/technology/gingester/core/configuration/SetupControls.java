package b.nana.technology.gingester.core.configuration;

import b.nana.technology.gingester.core.Node;
import b.nana.technology.gingester.core.reporting.Counter;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.List;
import java.util.Optional;

public final class SetupControls {

    private final Node node;
    private boolean requireOutgoingSync;
    private boolean requireOutgoingAsync;
    private Counter acksCounter;

    public SetupControls(Transformer<?, ?> transformer, Node node) {
        this.node = node;

        // if the transformer is sync-aware, then preconfigure this SetupControls to sync with __seed__
        if (transformer.isSyncAware()) {
            node.addSync("__seed__");
        }
    }



    public SetupControls maxWorkers(int maxWorkers) {
        node.maxWorkers(maxWorkers);
        return this;
    }

    public SetupControls maxQueueSize(int maxQueueSize) {
        node.maxQueueSize(maxQueueSize);
        return this;
    }

    public SetupControls maxBatchSize(int maxBatchSize) {
        node.maxBatchSize(maxBatchSize);
        return this;
    }

    public SetupControls links(List<String> links) {
        node.setLinks(links);
        return this;
    }

    public SetupControls syncs(List<String> syncs) {
        node.setSyncs(syncs);
        return this;
    }

    public SetupControls excepts(List<String> excepts) {
        node.setExcepts(excepts);
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

package b.nana.technology.gingester.core.configuration;

import b.nana.technology.gingester.core.reporting.Counter;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Phaser;

public final class SetupControls extends BaseConfiguration<SetupControls> {

    private final Map<String, Phaser> phasers;

    private boolean requireOutgoingSync;
    private boolean requireOutgoingAsync;
    private Counter acksCounter;



    public SetupControls(Map<String, Phaser> phasers) {
        this.phasers = phasers;
        links(Collections.singletonList("__maybe_next__"));
    }



    public SetupControls requireOutgoingSync() {
        this.requireOutgoingSync = true;
        return this;
    }

    public SetupControls requireOutgoingAsync() {
        this.requireOutgoingAsync = true;
        return this;
    }

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

    public Phaser getPhaser(String name) {
        return phasers.computeIfAbsent(name, x -> new Phaser());
    }
}

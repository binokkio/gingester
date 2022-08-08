package b.nana.technology.gingester.core.configuration;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.reporting.Counter;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public final class SetupControls extends BaseConfiguration<SetupControls> {

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

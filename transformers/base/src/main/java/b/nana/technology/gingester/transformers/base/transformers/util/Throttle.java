package b.nana.technology.gingester.transformers.base.transformers.util;

import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Names(1)
@Passthrough
@Example(example = "10", description = "Slow the flow down to a maximum of 10 items per second")
public final class Throttle implements Transformer<Object, Object> {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Semaphore semaphore;
    private final int perSecond;

    public Throttle(Parameters parameters) {
        perSecond = parameters.perSecond;
        semaphore = new Semaphore(perSecond, true);
    }

    @Override
    public void open() {
        scheduler.scheduleAtFixedRate(() -> {
            semaphore.drainPermits();
            semaphore.release(perSecond);
        }, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {
        semaphore.acquireUninterruptibly();
        out.accept(context, in);
    }

    @Override
    public void close() {
        scheduler.shutdown();
    }

    public static class Parameters {

        public int perSecond = 1;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(int perSecond) {
            this.perSecond = perSecond;
        }
    }
}

package b.nana.technology.gingester.transformers.base.transformers.util;

import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Names(1)
@Passthrough
@Example(example = "10", description = "Slow the flow down to a maximum of 10 items per second")
@Example(example = "'{\"permits\": 10, \"rate\": \"1M\"}'", description = "Slow the flow down to a maximum of 10 items per minute")
public final class Throttle implements Transformer<Object, Object> {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Semaphore semaphore;
    private final int permits;
    private final Duration rate;

    public Throttle(Parameters parameters) {
        permits = parameters.permits;
        semaphore = new Semaphore(permits, true);
        rate = Duration.parse("PT" + parameters.rate);
    }

    @Override
    public void open() {
        scheduler.scheduleAtFixedRate(() -> {
            semaphore.drainPermits();
            semaphore.release(permits);
        }, rate.toMillis(), rate.toMillis(), TimeUnit.MILLISECONDS);
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

        public int permits = 1;
        public String rate = "1S";

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(int permits) {
            this.permits = permits;
        }
    }
}

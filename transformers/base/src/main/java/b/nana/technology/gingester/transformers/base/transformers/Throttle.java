package b.nana.technology.gingester.transformers.base.transformers;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Passthrough;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Throttle<T> extends Passthrough<T> {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Semaphore semaphore;
    private final int perSecond;

    public Throttle(Parameters parameters) {
        super(parameters);
        perSecond = parameters.perSecond;
        semaphore = new Semaphore(perSecond, true);
    }

    @Override
    protected void setup(Setup setup) {
        setup.limitWorkers(1);
    }

    @Override
    protected void open() throws Exception {
        super.open();
        scheduler.scheduleAtFixedRate(() -> {
            semaphore.drainPermits();
            semaphore.release(perSecond);
        }, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    protected void transform(Context context, T input) throws Exception {
        semaphore.acquireUninterruptibly();
        emit(context, input);
    }

    @Override
    protected void close() throws Exception {
        scheduler.shutdown();
        super.close();
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

package b.nana.technology.gingester.transformers.base.transformers.groupby;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

@Passthrough
public final class Interval implements Transformer<Object, Object> {

    private final ContextMap<State> contextMap = new ContextMap<>();
    private final long intervalNanos;

    private ScheduledExecutorService scheduledExecutorService;

    public Interval(Parameters parameters) {
        intervalNanos = Duration.parse(requireNonNull(parameters.interval, "GroupByInterval must be given `interval` parameter")).toNanos();
    }

    @Override
    public void setup(SetupControls controls) {
        controls.syncs(List.of("__seed__"));
    }

    @Override
    public void open() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void prepare(Context context, Receiver<Object> out) throws Exception {
        contextMap.put(context, new State(context, out));
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        Context group = contextMap.act(context, State::getGroup);
        out.accept(context.extend().group(group), in);
    }

    @Override
    public void finish(Context context, Receiver<Object> out) throws Exception {
        contextMap.remove(context).close();
    }

    @Override
    public void close() {
        scheduledExecutorService.shutdown();
    }

    public static class Parameters {

        public String interval;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String interval) {
            this.interval = interval;
        }
    }

    private class State {

        private final Context parent;
        private final Receiver<Object> out;
        private final ScheduledFuture<?> scheduledFuture;

        private int currentInterval;
        private volatile Context currentGroup;

        private State(Context parent, Receiver<Object> out) {

            this.parent = parent;
            this.out = out;

            scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(
                    this::closeCurrentGroup,
                    intervalNanos,
                    intervalNanos,
                    TimeUnit.NANOSECONDS
            );
        }

        private Context getGroup() {
            Context result = currentGroup;
            if (result == null) {
                result = out.acceptGroup(parent.stash("interval", currentInterval++));
                currentGroup = result;
            }
            return result;
        }

        // synchronized because the scheduledExecutorService and finish can run parallel, and we need to prevent
        // the scheduledExecutorService call to set currentGroup to null and then the finish call to overtake which
        // could get the __seed__ finish signal to get ahead of the group finish signal
        private synchronized void closeCurrentGroup() {
            Context moribund = currentGroup;
            currentGroup = null;
            if (moribund != null) out.closeGroup(moribund);
        }

        private void close() {
            closeCurrentGroup();
            scheduledFuture.cancel(false);
        }
    }
}

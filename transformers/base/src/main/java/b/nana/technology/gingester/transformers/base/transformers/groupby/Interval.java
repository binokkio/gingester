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
import java.util.function.Consumer;

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
        contextMap.get(context).withCurrentGroup(group ->
                out.accept(context.group(group), in));
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

        private final Context groupParent;
        private final Receiver<Object> out;
        private final ScheduledFuture<?> scheduledFuture;

        private int currentInterval;
        private volatile Context currentGroup;

        private State(Context groupParent, Receiver<Object> out) {

            this.groupParent = groupParent;
            this.out = out;

            scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(
                    this::closeCurrentGroup,
                    intervalNanos,
                    intervalNanos,
                    TimeUnit.NANOSECONDS
            );
        }

        private synchronized void withCurrentGroup(Consumer<Context> consumer) {
            if (currentGroup == null) {
                currentGroup = out.acceptGroup(groupParent.stash("interval", currentInterval++));
            }
            consumer.accept(currentGroup);
        }

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

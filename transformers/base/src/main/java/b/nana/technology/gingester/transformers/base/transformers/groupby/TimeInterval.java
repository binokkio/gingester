package b.nana.technology.gingester.transformers.base.transformers.groupby;

import b.nana.technology.gingester.core.annotations.Example;
import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.annotations.Stashes;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

@Passthrough
@Stashes(stash = "interval", type = Integer.class)
@Stashes(stash = "intervalStart", type = ZonedDateTime.class)
@Example(example = "PT5M", description = "Group items per 5 minutes starting when the first item of a group arrives")
@Example(example = "PT5M fixedRate", description = "Group items in a new group every 5 minutes, omit empty groups")
@Example(example = "PT5M fixedRate yieldEmpty", description = "Group items in a new group every 5 minutes, yield empty groups")
@Example(example = "PT5M inactivity", description = "Group items together as long as they arrive within 5 minutes of each other")
public final class TimeInterval implements Transformer<Object, Object> {

    private final ContextMap<State> contextMap = new ContextMap<>();
    private final long intervalNanos;
    private final boolean fixedRate;
    private final boolean yieldEmpty;
    private final boolean inactivity;
    private final ZoneId zoneId;

    private ScheduledExecutorService scheduledExecutorService;

    public TimeInterval(Parameters parameters) {

        intervalNanos = Duration.parse(requireNonNull(parameters.interval, "GroupByTimeInterval must be given `interval` parameter")).toNanos();
        fixedRate = parameters.fixedRate;
        yieldEmpty = parameters.yieldEmpty;
        inactivity = parameters.inactivity;
        zoneId = parameters.zone != null ? ZoneId.of(parameters.zone) : ZoneId.systemDefault();

        if (!fixedRate && yieldEmpty)
            throw new IllegalArgumentException("Combination of fixedRate=false and yieldEmpty=true not supported");

        if (fixedRate && inactivity)
            throw new IllegalArgumentException("Combination of fixedRate=true and inactivity=true not supported");
    }

    @Override
    public void open() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void prepare(Context context, Receiver<Object> out) {
        contextMap.put(context, new State(context, out));
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {
        contextMap.get(context).withCurrentGroup(group ->
                out.accept(context.group(group), in));
    }

    @Override
    public void finish(Context context, Receiver<Object> out) {
        contextMap.remove(context).close();
    }

    @Override
    public void close() {
        scheduledExecutorService.shutdown();
    }

    private class State {

        private final Context groupParent;
        private final Receiver<Object> out;

        private int currentInterval;
        private volatile Context currentGroup;
        private ScheduledFuture<?> scheduledFuture;

        private State(Context groupParent, Receiver<Object> out) {

            this.groupParent = groupParent;
            this.out = out;

            if (fixedRate) {

                scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(
                        this::closeCurrentGroup,
                        intervalNanos,
                        intervalNanos,
                        TimeUnit.NANOSECONDS
                );

                if (yieldEmpty)
                    getCurrentGroup();
            }
        }

        private synchronized void withCurrentGroup(Consumer<Context> consumer) {
            consumer.accept(getCurrentGroup());

            if (inactivity) {
                scheduledFuture.cancel(false);
                scheduledFuture = scheduledExecutorService.schedule(
                        this::closeCurrentGroup,
                        intervalNanos,
                        TimeUnit.NANOSECONDS
                );
            }
        }

        private synchronized Context getCurrentGroup() {

            if (currentGroup == null) {

                currentGroup = out.acceptGroup(groupParent.stash(Map.of(
                        "interval", currentInterval++,
                        "intervalStart", Instant.now().atZone(zoneId)
                )));

                if (!fixedRate) {
                    scheduledFuture = scheduledExecutorService.schedule(
                            this::closeCurrentGroup,
                            intervalNanos,
                            TimeUnit.NANOSECONDS
                    );
                }
            }

            return currentGroup;
        }

        private synchronized void closeCurrentGroup() {
            if (currentGroup != null) {
                out.closeGroup(currentGroup);
                currentGroup = null;
                if (yieldEmpty)
                    getCurrentGroup();
            }
        }

        private synchronized void close() {
            if (currentGroup != null) {
                out.closeGroup(currentGroup);
                currentGroup = null;
                scheduledFuture.cancel(false);
            }
        }
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, interval -> o("interval", interval));
                rule(JsonNode::isArray, array -> {
                    boolean fixedRate = false;
                    boolean yieldEmpty = false;
                    boolean inactivity = false;
                    String zone = null;
                    for (int i = 1; i < array.size(); i++) {
                        JsonNode element = array.get(i);
                        if (element.asText().equals("fixedRate")) {
                            fixedRate = true;
                        } else if (element.asText().equals("yieldEmpty")) {
                            yieldEmpty = true;
                        } else if (element.asText().equals("inactivity")) {
                            inactivity = true;
                        } else if (zone == null) {
                            zone = element.asText();
                        } else {
                            throw new IllegalArgumentException("GroupByTimeInterval parameter parsing failed at " + element);
                        }
                    }
                    return o(
                            "interval", array.get(0),
                            "fixedRate", fixedRate,
                            "yieldEmpty", yieldEmpty,
                            "inactivity", inactivity,
                            "zone", zone
                    );
                });
            }
        }

        public String interval;
        public boolean fixedRate;
        public boolean yieldEmpty;
        public boolean inactivity;
        public String zone;
    }
}

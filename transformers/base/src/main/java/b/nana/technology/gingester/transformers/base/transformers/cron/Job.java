package b.nana.technology.gingester.transformers.base.transformers.cron;

import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class Job implements Transformer<Object, ZonedDateTime> {

    private final List<CronExpression> schedules;
    private final boolean skips;
    private final ZoneId zoneId;

    public Job(Parameters parameters) {
        schedules = parameters.schedules.stream().map(CronExpression::create).collect(Collectors.toList());
        skips = parameters.skips;
        zoneId = parameters.zone == null ? ZoneId.systemDefault() : ZoneId.of(parameters.zone);
    }

    @Override
    public void setup(SetupControls controls) {
        controls.maxWorkers(1);
        controls.maxBatchSize(1);
    }

    @Override
    public void transform(Context context, Object in, Receiver<ZonedDateTime> out) throws InterruptedException {

        ZonedDateTime anchor = now();

        while (true) {

            ZonedDateTime next = null;
            for (CronExpression schedule : schedules) {
                ZonedDateTime option = schedule.nextTimeAfter(anchor);
                if (next == null || option.isBefore(next)) {
                    next = option;
                }
            }

            Duration duration = Duration.between(Instant.now(), next.toInstant());

            if (!duration.isNegative()) {
                Thread.sleep(duration.getSeconds() * 1000, duration.getNano() / 1000);
            }

            out.accept(
                    context.stash(Map.of(
                            "description", next.toString(),
                            "time", Map.of(
                                    "year", next.getYear(),
                                    "month", next.getMonthValue(),
                                    "day", next.getDayOfMonth(),
                                    "hour", next.getHour(),
                                    "minute", next.getMinute(),
                                    "second", next.getSecond(),
                                    "milli", next.getNano() / 1_000_000,
                                    "nano", next.getNano() % 1_000_000
                            )
                    )),
                    next
            );

            ZonedDateTime now = now();

            if (now.isBefore(next) || !skips) {
                anchor = next;
            } else {
                anchor = now;
            }
        }
    }

    private ZonedDateTime now() {
        return Instant.now().atZone(zoneId);
    }

    public static class Parameters {

        public List<String> schedules;
        public boolean skips = true;
        public String zone;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(List<String> schedules) {
            this.schedules = schedules;
        }
    }
}

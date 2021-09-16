package b.nana.technology.gingester.transformers.base.transformers.cron;

import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;

public class Job implements Transformer<Object, Object> {

    private final String schedule;
    private final boolean skips;

    public Job(Parameters parameters) {
        schedule = parameters.schedule;
        skips = parameters.skips;
    }

    @Override
    public void setup(SetupControls controls) {
        controls.maxWorkers(1);
        controls.maxBatchSize(1);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws InterruptedException {

        CronExpression cronExpression = CronExpression.create(schedule);
        ZonedDateTime anchor = ZonedDateTime.now();

        while (true) {

            ZonedDateTime next = cronExpression.nextTimeAfter(anchor);
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
                    new Object()  // TODO
            );

            ZonedDateTime now = ZonedDateTime.now();

            if (now.isBefore(next) || !skips) {
                anchor = next;
            } else {
                anchor = now;
            }
        }
    }

    public static class Parameters {

        public String schedule;
        public boolean skips = true;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String schedule) {
            this.schedule = schedule;
        }
    }
}

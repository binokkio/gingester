package b.nana.technology.gingester.transformers.base.transformers.cron;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Job extends Transformer<Void, Void> {

    private final String schedule;
    private final boolean skips;
    private final AtomicBoolean triggered = new AtomicBoolean();

    public Job(Parameters parameters) {
        super(parameters);
        schedule = parameters.schedule;
        skips = parameters.skips;
    }

    @Override
    protected void setup(Setup setup) {
        setup.assertNoUpstream();
        setup.limitWorkers(1);
        setup.limitBatchSize(1);
    }

    @Override
    protected void transform(Context context, Void input) throws Exception {

        CronExpression cronExpression = CronExpression.create(schedule);
        ZonedDateTime anchor = ZonedDateTime.now();

        while (true) {

            ZonedDateTime next = cronExpression.nextTimeAfter(anchor);
            Duration duration = Duration.between(Instant.now(), next.toInstant());
            long durationSeconds = Math.round(duration.getSeconds() + duration.getNano() / 1_000_000_000f);

            if (durationSeconds > 0) {

                getThreader().schedule(
                        this::trigger,
                        durationSeconds,
                        TimeUnit.SECONDS
                );

                // TODO it is no longer necessary to emit from the transform-calling thread
                synchronized (triggered) {
                    while (!triggered.get()) {
                        triggered.wait();
                        if (triggered.get()) {
                            triggered.set(false);
                            break;
                        }
                    }
                }
            }

            emit(
                    context.extend(this)
                            .description(next.toString())
                            .details(Map.of(
                                    "year", next.getYear(),
                                    "month", next.getMonthValue(),
                                    "day", next.getDayOfMonth(),
                                    "hour", next.getHour(),
                                    "minute", next.getMinute(),
                                    "second", next.getSecond()
                            )),
                    null
            );

            ZonedDateTime now = ZonedDateTime.now();

            if (now.isBefore(next) || !skips) {
                anchor = next;
            } else {
                anchor = now;
            }
        }
    }

    private void trigger() {
        synchronized (triggered) {
            triggered.set(true);
            triggered.notify();
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
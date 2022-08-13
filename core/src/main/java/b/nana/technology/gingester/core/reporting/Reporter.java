package b.nana.technology.gingester.core.reporting;

import b.nana.technology.gingester.core.FlowRunner;
import b.nana.technology.gingester.core.controller.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class Reporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowRunner.class);

    private final ThreadFactory threadFactory = runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("Gingester-Reporter");
        return thread;
    };

    private final int intervalMillis;
    private final Map<Controller<?, ?>, Sampler> targets;

    private ScheduledExecutorService executorService;

    public Reporter(int intervalSeconds, Collection<Controller<?, ?>> targets) {
        this.intervalMillis = intervalSeconds * 1000;
        this.targets = targets.stream().filter(c -> c.report).collect(Collectors.toMap(
                c -> c,
                c -> new Sampler(c.acks != null ? c.acks : c.dealt),
                (a, b) -> { throw new UnsupportedOperationException("Not implemented"); },
                LinkedHashMap::new
        ));
    }

    public void start() {
        executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
        targets.forEach((c, s) -> s.epoch());
        executorService.scheduleAtFixedRate(this::report, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        executorService.shutdown();
        report();
    }

    private void report() {
        targets.forEach((controller, sampler) -> {
            sampler.sample();
            if (LOGGER.isInfoEnabled()) {

                String customMessage = controller.transformer.onReport();

                LOGGER.info(String.format(
                        "%s: %,d processed at %,.2f/s (%s), %,.2f/s (%s)",
                        controller.id,
                        sampler.getValue(),
                        sampler.getCurrentChangePerSecond(),
                        humanize(sampler.getCurrentNanos()),
                        sampler.getEpochChangePerSecond(),
                        humanize(sampler.getEpochNanos())
                ));

                if (!customMessage.isEmpty()) {
                    LOGGER.info(controller.id + ": " + customMessage);
                }
            }
        });
    }

    private String humanize(long nanos) {
        long asSeconds = Math.round(nanos / 1_000_000_000d);
        long days = asSeconds / 86400;
        long hours = asSeconds % 86400 / 3600;
        long minutes = asSeconds % 86400 % 3600 / 60;
        long seconds = asSeconds % 86400 % 3600 % 60;
        if (days != 0) {
            return days + "d" + hours + "h" + minutes + "m" + seconds + "s";
        } else if (hours != 0) {
            return hours + "h" + minutes + "m" + seconds + "s";
        } else if (minutes != 0) {
            return minutes + "m" + seconds + "s";
        } else {
            return seconds + "s";
        }
    }
}

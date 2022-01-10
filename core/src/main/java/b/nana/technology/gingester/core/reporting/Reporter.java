package b.nana.technology.gingester.core.reporting;

import b.nana.technology.gingester.core.Gingester;
import b.nana.technology.gingester.core.controller.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class Reporter extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(Gingester.class);

    private final int intervalMillis;
    private final Map<Controller<?, ?>, Sampler> targets;

    public Reporter(int intervalSeconds, Collection<Controller<?, ?>> targets) {
        setName("Gingester-Reporter");
        this.intervalMillis = intervalSeconds * 1000;
        this.targets = targets.stream().filter(c -> c.report).collect(Collectors.toMap(
                c -> c,
                c -> new Sampler(c.acks != null ? c.acks : c.dealt),
                (a, b) -> { throw new UnsupportedOperationException("Not implemented"); },
                LinkedHashMap::new
        ));
    }

    @Override
    public void run() {

        targets.forEach((c, s) -> s.epoch());

        while (true) {

            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                break;
            }

            report();
        }

        report();
    }

    private void report() {
        targets.forEach((controller, sampler) -> {
            sampler.sample();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format(
                        "%s: %,d processed at %,.2f/s (%s), %,.2f/s (%s)",
                        controller.id,
                        sampler.getValue(),
                        sampler.getCurrentChangePerSecond(),
                        humanize(sampler.getCurrentNanos()),
                        sampler.getEpochChangePerSecond(),
                        humanize(sampler.getEpochNanos())
                ));
            }
        });
    }

    private String humanize(long nanos) {
        long asSeconds = nanos / 1_000_000_000;
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

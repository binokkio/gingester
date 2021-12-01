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
                c -> new Sampler(c.acks != null ? c.acks : c.delt),
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
                        "%s: %,d processed at %,.2f/s over the last %,.0f seconds, %,.2f/s overall",
                        controller.id,
                        sampler.getValue(),
                        sampler.getCurrentChangePerSecond(),
                        sampler.getCurrentNanos() / 1_000_000_000d,
                        sampler.getEpochChangePerSecond()
                ));
            }
        });
    }
}

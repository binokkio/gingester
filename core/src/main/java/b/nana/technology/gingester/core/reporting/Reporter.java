package b.nana.technology.gingester.core.reporting;

import b.nana.technology.gingester.core.controller.Controller;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Reporter extends Thread {

    private final Map<Controller<?, ?>, Sampler> targets;

    public Reporter(Collection<Controller<?, ?>> targets) {
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
                Thread.sleep(2000);
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

            System.err.printf(
                    "%s: %,d processed at %,.2f/s over the last %,.0f seconds, %,.2f/s overall%n",
                    controller.id,
                    sampler.getValue(),
                    sampler.getCurrentChangePerSecond(),
                    sampler.getCurrentNanos() / 1_000_000_000d,
                    sampler.getEpochChangePerSecond()
            );
        });
    }
}

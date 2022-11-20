package b.nana.technology.gingester.transformers.git.giterate;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.StashDetails;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.Map;

final class TimeIntervalGiterator implements Giterator {

    private final Runtime runtime = Runtime.getRuntime();
    private final TemporalAmount interval;

    TimeIntervalGiterator(String intervalString) {
        if (intervalString.startsWith("PT")) {
            interval = Duration.parse(intervalString);
        } else {
            interval = Period.parse(intervalString);
        }
    }

    @Override
    public StashDetails getStashDetails() {
        return StashDetails.of(
                "commit", String.class,
                "actual", ZonedDateTime.class,
                "target", ZonedDateTime.class
        );
    }

    @Override
    public void giterate(Path clone, List<Commit> commits, Context context, Receiver<Path> out) throws IOException, InterruptedException {

        ZonedDateTime target = commits.get(0).time;
        String current = null;

        for (int i = 0; i < commits.size() - 1; i++) {

            Commit a = commits.get(i);
            Commit b = commits.get(i + 1);

            if (b.time.isAfter(target)) {

                if (!a.hash.equals(current)) {
                    Process checkoutProcess = runtime.exec(new String[]{"git", "checkout", a.hash}, null, clone.toFile());
                    int checkoutResult = checkoutProcess.waitFor();
                    if (checkoutResult != 0) throw new IllegalStateException("git checkout did not exit with 0");
                    current = a.hash;
                }

                out.accept(
                        context.stash(Map.of(
                                "commit", a.hash,
                                "actual", a.time,
                                "target", target
                        )),
                        clone
                );

                target = target.plus(interval);
                i--;
            }
        }
    }
}

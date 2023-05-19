package b.nana.technology.gingester.core.reporting;

import java.text.DecimalFormat;
import java.util.function.Function;

public class SamplerFormatter {

    private static final DecimalFormat DEFAULT_SAMPLE_FORMATTER = new DecimalFormat("#,##0.##");

    private final String subject;
    private final Function<Double, String> sampleFormatter;
    private final Function<Long, String> nanosFormatter;

    public SamplerFormatter(String subject) {
        this(
                subject,
                DEFAULT_SAMPLE_FORMATTER::format
        );
    }

    public SamplerFormatter(String subject, Function<Double, String> sampleFormatter) {
        this(
                subject,
                sampleFormatter,
                SamplerFormatter::humanize
        );
    }

    public SamplerFormatter(String subject, Function<Double, String> sampleFormatter, Function<Long, String> nanosFormatter) {
        this.subject = subject;
        this.sampleFormatter = sampleFormatter;
        this.nanosFormatter = nanosFormatter;
    }

    public String format(Sampler sampler) {
        return String.format(
                sampler.isEpochInteresting() ?
                        "%s %s at %s/s (%s), %s/s (%s)" :
                        "%s %s at %s/s (%s)",
                sampleFormatter.apply((double) sampler.getValue()),
                subject,
                sampleFormatter.apply(sampler.getCurrentChangePerSecond()),
                nanosFormatter.apply(sampler.getCurrentNanos()),
                sampleFormatter.apply(sampler.getEpochChangePerSecond()),
                nanosFormatter.apply(sampler.getEpochNanos())
        );
    }

    private static String humanize(long nanos) {
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

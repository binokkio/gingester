package b.nana.technology.gingester.core.reporting;

import java.text.DecimalFormat;
import java.util.function.Function;

public class SamplerFormatter {

    public static final DecimalFormat DEFAULT_SAMPLE_FORMATTER = new DecimalFormat("#,##0.##");
    private static final DurationFormatter durationFormatter = new DurationFormatter(true);

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
                durationFormatter::format
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
}

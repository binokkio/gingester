package b.nana.technology.gingester.core.reporting;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationFormatter {

    private static final Pattern pattern = Pattern.compile("(\\d+)(\\D+)");

    private final boolean includeTrailingZeros;

    public DurationFormatter() {
        this(false);
    }

    public DurationFormatter(boolean includeTrailingZeros) {
        this.includeTrailingZeros = includeTrailingZeros;
    }

    public Duration parse(String input) {

        // try strict parsing first
        try {
            return Duration.parse(input);
        } catch (DateTimeParseException e) {
            // ignore, try lenient parsing below
        }

        // lenient parsing
        Duration duration = Duration.ZERO;
        Matcher matcher = pattern.matcher(input);
        int end = -1;

        while (matcher.find()) {

            int value = Integer.parseInt(matcher.group(1));
            String type = matcher.group(2);

            duration = switch (type) {
                case "d" -> duration.plus(Duration.ofDays(value));
                case "h" -> duration.plus(Duration.ofHours(value));
                case "m" -> duration.plus(Duration.ofMinutes(value));
                case "s" -> duration.plus(Duration.ofSeconds(value));
                case "ms" -> duration.plus(Duration.ofMillis(value));
                default -> throw new IllegalArgumentException("Invalid duration format: " + input);
            };

            end = matcher.end();
        }

        if (end != input.length())
            throw new IllegalArgumentException("Invalid duration format: " + input);

        return duration;
    }

    public String format(Duration duration) {
        return format(duration.toNanos());
    }

    public String format(long nanos) {

        long asSeconds = Math.round(nanos / 1_000_000_000d);
        long days = asSeconds / 86400;
        long hours = asSeconds % 86400 / 3600;
        long minutes = asSeconds % 86400 % 3600 / 60;
        long seconds = asSeconds % 86400 % 3600 % 60;

        StringBuilder stringBuilder = new StringBuilder();

        if (days != 0)
            stringBuilder.append(days).append('d');

        if (hours != 0 || (includeTrailingZeros && !stringBuilder.isEmpty()))
            stringBuilder.append(hours).append('h');

        if (minutes != 0 || (includeTrailingZeros && !stringBuilder.isEmpty()))
            stringBuilder.append(minutes).append('m');

        if (seconds != 0 || includeTrailingZeros || stringBuilder.isEmpty())
            stringBuilder.append(seconds).append('s');

        return stringBuilder.toString();
    }
}

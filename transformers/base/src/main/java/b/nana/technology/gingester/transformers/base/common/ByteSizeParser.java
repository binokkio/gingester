package b.nana.technology.gingester.transformers.base.common;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ByteSizeParser {

    private static final String PREFIXES = "kmgtpezy";
    private static final Pattern BYTE_SIZE_PATTERN = Pattern.compile("(?i)([\\d.]+)(?:([kmgtpezy]?)([i]?)b)?");

    private ByteSizeParser() {}

    public static long parse(String byteSize) {
        Matcher matcher = BYTE_SIZE_PATTERN.matcher(byteSize.toLowerCase(Locale.ENGLISH));
        if (!matcher.matches()) throw new IllegalArgumentException("Invalid byteSize format");
        double value = Double.parseDouble(matcher.group(1));
        int base = matcher.group(3).isEmpty() ? 1000 : 1024;
        int exponent = PREFIXES.indexOf(matcher.group(2)) + 1;
        return (long) (value * Math.pow(base, exponent));
    }
}

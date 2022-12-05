package b.nana.technology.gingester.core.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ByteCountFormat {

    private static final String PREFIXES = "KMGTPEZY";
    private static final Pattern BYTE_COUNT_PATTERN = Pattern.compile("([\\d.]+)(?:([KMGTPEZY]?)(i?)B)?");
    private static final BigDecimal BD1000 = BigDecimal.valueOf(1000);
    private static final BigDecimal BD1024 = BigDecimal.valueOf(1024);

    public long parse(String byteCount) {
        Matcher matcher = BYTE_COUNT_PATTERN.matcher(byteCount);
        if (!matcher.matches()) throw new IllegalArgumentException("Invalid byteCount format");
        double value = Double.parseDouble(matcher.group(1));
        if (matcher.group(2) == null) return (long) value;
        int base = matcher.group(3).isEmpty() ? 1000 : 1024;
        String prefix = matcher.group(2);
        int exponent = prefix.isEmpty() ? 0 : PREFIXES.indexOf(prefix) + 1;
        return (long) (value * Math.pow(base, exponent));
    }

    public String format(long byteCount) {
        return format(BigDecimal.valueOf(byteCount), false);
    }

    public String formatBinary(long byteCount) {
        return format(BigDecimal.valueOf(byteCount), true);
    }

    public String format(double byteCount) {
        return format(BigDecimal.valueOf(byteCount), false);
    }

    public String formatBinary(double byteCount) {
        return format(BigDecimal.valueOf(byteCount), true);
    }

    private String format(BigDecimal byteCount, boolean binary) {
        if (byteCount.signum() == 0) return "0.00B";
        BigDecimal base = binary ? BD1024 : BD1000;
        BigDecimal quotient;
        int exponent = -1;
        while ((quotient = divideWithSignificantDigits(byteCount, base.pow(++exponent), 3)).compareTo(BD1000) >= 0 && exponent < PREFIXES.length());
        String prefix = exponent == 0 ? "" : PREFIXES.charAt(exponent - 1) + (binary ? "i" : "");
        return quotient + prefix + "B";
    }

    private BigDecimal divideWithSignificantDigits(BigDecimal dividend, BigDecimal divisor, int significantDigits) {
        BigDecimal quotient = dividend.divide(divisor, significantDigits, RoundingMode.HALF_UP);
        quotient = quotient.setScale(quotient.scale() + significantDigits - quotient.precision(), RoundingMode.HALF_UP);
        quotient = quotient.setScale(quotient.scale() + significantDigits - quotient.precision(), RoundingMode.HALF_UP);  // repeated to address added significant digit when e.g. 9.99 got rounded up in the previous step
        return quotient;
    }
}

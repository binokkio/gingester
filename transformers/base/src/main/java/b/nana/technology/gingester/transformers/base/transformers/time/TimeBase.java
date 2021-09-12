package b.nana.technology.gingester.transformers.base.transformers.time;

import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Optional;

public abstract class TimeBase<I, O> implements Transformer<I, O> {

    private final DateTimeFormatterBuilder formatBuilder;
    private final ZoneId zoneId;

    public TimeBase(Parameters parameters) {
        formatBuilder = getFormatBuilder(parameters.format);
        zoneId = parameters.zone != null ? ZoneId.of(parameters.zone) : null;
    }

    private DateTimeFormatterBuilder getFormatBuilder(String format) {
        switch (format) {
            case "ISO_LOCAL_DATE": return new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_LOCAL_DATE);
            case "ISO_OFFSET_DATE": return new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_OFFSET_DATE);
            case "ISO_DATE": return new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_DATE);
            case "ISO_LOCAL_TIME": return new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_LOCAL_TIME);
            case "ISO_OFFSET_TIME": return new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_OFFSET_TIME);
            case "ISO_TIME": return new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_TIME);
            case "ISO_LOCAL_DATE_TIME": return new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            case "ISO_OFFSET_DATE_TIME": return new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            case "ISO_ZONED_DATE_TIME": return new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_ZONED_DATE_TIME);
            case "ISO_DATE_TIME": return new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_DATE_TIME);
            case "ISO_ORDINAL_DATE": return new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_ORDINAL_DATE);
            case "ISO_WEEK_DATE": return new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_WEEK_DATE);
            case "ISO_INSTANT": return new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_INSTANT);
            case "BASIC_ISO_DATE": return new DateTimeFormatterBuilder().append(DateTimeFormatter.BASIC_ISO_DATE);
            case "RFC_1123_DATE_TIME": return new DateTimeFormatterBuilder().append(DateTimeFormatter.RFC_1123_DATE_TIME);
            default: return new DateTimeFormatterBuilder().appendPattern(format);
        }
    }

    protected DateTimeFormatterBuilder getFormatBuilder() {
        return formatBuilder;
    }

    protected final Optional<ZoneId> getZoneId() {
        return Optional.ofNullable(zoneId);
    }

    public static class Parameters {

        public String format;
        public String zone;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String format) {
            this.format = format;
        }
    }
}

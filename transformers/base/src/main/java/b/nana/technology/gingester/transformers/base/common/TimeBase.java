package b.nana.technology.gingester.transformers.base.common;

import b.nana.technology.gingester.core.Transformer;
import b.nana.technology.gingester.transformers.base.transformers.string.ToTime;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.format.DateTimeFormatter;

public abstract class TimeBase<I, O> extends Transformer<I, O> {

    private final DateTimeFormatter formatter;

    public TimeBase(ToTime.Parameters parameters) {
        super(parameters);
        formatter = getFormatter(parameters.format);
    }

    private DateTimeFormatter getFormatter(String format) {
        switch (format) {
            case "ISO_LOCAL_DATE": return DateTimeFormatter.ISO_LOCAL_DATE;
            case "ISO_OFFSET_DATE": return DateTimeFormatter.ISO_OFFSET_DATE;
            case "ISO_DATE": return DateTimeFormatter.ISO_DATE;
            case "ISO_LOCAL_TIME": return DateTimeFormatter.ISO_LOCAL_TIME;
            case "ISO_OFFSET_TIME": return DateTimeFormatter.ISO_OFFSET_TIME;
            case "ISO_TIME": return DateTimeFormatter.ISO_TIME;
            case "ISO_LOCAL_DATE_TIME": return DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            case "ISO_OFFSET_DATE_TIME": return DateTimeFormatter.ISO_OFFSET_DATE_TIME;
            case "ISO_ZONED_DATE_TIME": return DateTimeFormatter.ISO_ZONED_DATE_TIME;
            case "ISO_DATE_TIME": return DateTimeFormatter.ISO_DATE_TIME;
            case "ISO_ORDINAL_DATE": return DateTimeFormatter.ISO_ORDINAL_DATE;
            case "ISO_WEEK_DATE": return DateTimeFormatter.ISO_WEEK_DATE;
            case "ISO_INSTANT": return DateTimeFormatter.ISO_INSTANT;
            case "BASIC_ISO_DATE": return DateTimeFormatter.BASIC_ISO_DATE;
            case "RFC_1123_DATE_TIME": return DateTimeFormatter.RFC_1123_DATE_TIME;
            default: return DateTimeFormatter.ofPattern(format);
        }
    }

    protected final DateTimeFormatter getFormatter() {
        return formatter;
    }

    public static class Parameters {

        public String format;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String format) {
            this.format = format;
        }
    }
}

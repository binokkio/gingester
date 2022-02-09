package b.nana.technology.gingester.transformers.base.transformers.time;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class FromString extends TimeBase<String, TemporalAccessor> {

    private final DateTimeFormatter formatter;

    public FromString(Parameters parameters) {
        super(parameters);

        DateTimeFormatterBuilder builder = getFormatBuilder();

        parameters.defaults.forEach((key, value) -> {
            TemporalField temporalField = FIELD_MAP.get(key);
            if (temporalField == null) throw new IllegalArgumentException("Unknown default: " + key);
            builder.parseDefaulting(temporalField, value);
        });

        DateTimeFormatter formatter = builder.toFormatter();

        ZoneId zoneId = getZoneId().orElse(null);
        if (zoneId != null) {
            formatter = formatter.withZone(zoneId);
        }

        this.formatter = formatter;
    }

    @Override
    public void transform(Context context, String in, Receiver<TemporalAccessor> out) throws Exception {
        out.accept(context, formatter.parse(in));
    }

    public static class Parameters extends TimeBase.Parameters {

        public Map<String, Integer> defaults = Collections.emptyMap();

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String format) {
            super(format);
        }
    }

    private static final Map<String, TemporalField> FIELD_MAP = new HashMap<>();
    static {
        FIELD_MAP.put("alignedDayOfWeekInMonth", ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH);
        FIELD_MAP.put("alignedDayOfWeekInYear", ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR);
        FIELD_MAP.put("alignedWeekOfMonth", ChronoField.ALIGNED_WEEK_OF_MONTH);
        FIELD_MAP.put("alignedWeekOfYear", ChronoField.ALIGNED_WEEK_OF_YEAR);
        FIELD_MAP.put("amPmOfDay", ChronoField.AMPM_OF_DAY);
        FIELD_MAP.put("clockHourOfAmPm", ChronoField.CLOCK_HOUR_OF_AMPM);
        FIELD_MAP.put("clockHourOfDay", ChronoField.CLOCK_HOUR_OF_DAY);
        FIELD_MAP.put("dayOfMonth", ChronoField.DAY_OF_MONTH);
        FIELD_MAP.put("dayOfQuarter", IsoFields.DAY_OF_QUARTER);
        FIELD_MAP.put("dayOfWeek", ChronoField.DAY_OF_WEEK);
        FIELD_MAP.put("dayOfYear", ChronoField.DAY_OF_YEAR);
        FIELD_MAP.put("epochDay", ChronoField.EPOCH_DAY);
        FIELD_MAP.put("era", ChronoField.ERA);
        FIELD_MAP.put("hourOfAmPm", ChronoField.HOUR_OF_AMPM);
        FIELD_MAP.put("hourOfDay", ChronoField.HOUR_OF_DAY);
        FIELD_MAP.put("instantSeconds", ChronoField.INSTANT_SECONDS);
        FIELD_MAP.put("julianDay", JulianFields.JULIAN_DAY);
        FIELD_MAP.put("microOfDay", ChronoField.MICRO_OF_DAY);
        FIELD_MAP.put("microOfSecond", ChronoField.MICRO_OF_SECOND);
        FIELD_MAP.put("milliOfDay", ChronoField.MILLI_OF_DAY);
        FIELD_MAP.put("milliOfSecond", ChronoField.MILLI_OF_SECOND);
        FIELD_MAP.put("minuteOfDay", ChronoField.MINUTE_OF_DAY);
        FIELD_MAP.put("minuteOfHour", ChronoField.MINUTE_OF_HOUR);
        FIELD_MAP.put("modifiedJulianDay", JulianFields.MODIFIED_JULIAN_DAY);
        FIELD_MAP.put("monthOfYear", ChronoField.MONTH_OF_YEAR);
        FIELD_MAP.put("nanoOfDay", ChronoField.NANO_OF_DAY);
        FIELD_MAP.put("nanoOfSecond", ChronoField.NANO_OF_SECOND);
        FIELD_MAP.put("offsetSeconds", ChronoField.OFFSET_SECONDS);
        FIELD_MAP.put("prolepticMonth", ChronoField.PROLEPTIC_MONTH);
        FIELD_MAP.put("quarterOfYear", IsoFields.QUARTER_OF_YEAR);
        FIELD_MAP.put("rataDie", JulianFields.RATA_DIE);
        FIELD_MAP.put("secondOfDay", ChronoField.SECOND_OF_DAY);
        FIELD_MAP.put("secondOfMinute", ChronoField.SECOND_OF_MINUTE);
        FIELD_MAP.put("weekBasedYear", IsoFields.WEEK_BASED_YEAR);
        FIELD_MAP.put("weekOfWeekBasedYear", IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        FIELD_MAP.put("year", ChronoField.YEAR);
        FIELD_MAP.put("yearOfEra", ChronoField.YEAR_OF_ERA);

    }
}

package b.nana.technology.gingester.transformers.base.transformers.time;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.transformers.base.common.TimeBase;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.util.HashMap;
import java.util.Map;

public class ToString extends TimeBase<TemporalAccessor, String> {

    private final ZoneId zonedId;

    public ToString(Parameters parameters) {
        super(parameters);
        zonedId = parameters.zone != null ?
                ZoneId.of(parameters.zone) :
                null;
    }

    @Override
    protected void transform(Context context, TemporalAccessor input) throws Exception {
        emit(context, getFormatter().format(input));
    }

    public static class Parameters extends TimeBase.Parameters {

        public String zone;
        public Map<String, Integer> defaults;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String format) {
            super(format);
        }
    }

    private static final Map<Character, TemporalField> FIELD_MAP = new HashMap<>();
    static {
        FIELD_MAP.put('G', ChronoField.ERA);
        FIELD_MAP.put('y', ChronoField.YEAR_OF_ERA);
        FIELD_MAP.put('u', ChronoField.YEAR);
        FIELD_MAP.put('Q', IsoFields.QUARTER_OF_YEAR);
        FIELD_MAP.put('q', IsoFields.QUARTER_OF_YEAR);
        FIELD_MAP.put('M', ChronoField.MONTH_OF_YEAR);
        FIELD_MAP.put('L', ChronoField.MONTH_OF_YEAR);
        FIELD_MAP.put('D', ChronoField.DAY_OF_YEAR);
        FIELD_MAP.put('d', ChronoField.DAY_OF_MONTH);
        FIELD_MAP.put('F', ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH);
        FIELD_MAP.put('E', ChronoField.DAY_OF_WEEK);
        FIELD_MAP.put('c', ChronoField.DAY_OF_WEEK);
        FIELD_MAP.put('e', ChronoField.DAY_OF_WEEK);
        FIELD_MAP.put('a', ChronoField.AMPM_OF_DAY);
        FIELD_MAP.put('H', ChronoField.HOUR_OF_DAY);
        FIELD_MAP.put('k', ChronoField.CLOCK_HOUR_OF_DAY);
        FIELD_MAP.put('K', ChronoField.HOUR_OF_AMPM);
        FIELD_MAP.put('h', ChronoField.CLOCK_HOUR_OF_AMPM);
        FIELD_MAP.put('m', ChronoField.MINUTE_OF_HOUR);
        FIELD_MAP.put('s', ChronoField.SECOND_OF_MINUTE);
        FIELD_MAP.put('S', ChronoField.NANO_OF_SECOND);
        FIELD_MAP.put('A', ChronoField.MILLI_OF_DAY);
        FIELD_MAP.put('n', ChronoField.NANO_OF_SECOND);
        FIELD_MAP.put('N', ChronoField.NANO_OF_DAY);
    }
}

package b.nana.technology.gingester.transformers.base.transformers.time;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.transformers.base.common.TimeBase;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ToString extends TimeBase<TemporalAccessor, String> {

    private final DateTimeFormatter formatter;

    public ToString(Parameters parameters) {
        super(parameters);
        DateTimeFormatter formatter = getFormatBuilder().toFormatter();
        ZoneId zoneId = getZoneId().orElse(null);
        if (zoneId != null) {
            formatter = formatter.withZone(zoneId);
        }
        this.formatter = formatter;
    }

    @Override
    protected void transform(Context context, TemporalAccessor input) throws Exception {
        emit(context, formatter.format(input));
    }
}

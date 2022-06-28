package b.nana.technology.gingester.transformers.base.transformers.time;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public final class ToString extends TimeBase<TemporalAccessor, String> {

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
    public void transform(Context context, TemporalAccessor in, Receiver<String> out) {
        out.accept(context, formatter.format(in));
    }
}

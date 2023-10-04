package b.nana.technology.gingester.transformers.base.transformers.time;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;

import java.time.temporal.TemporalAccessor;
import java.util.Date;

public final class FromDate extends InstantBase<Date> {

    public FromDate(Parameters parameters) {
        super(parameters);
    }

    @Override
    public void transform(Context context, Date in, Receiver<TemporalAccessor> out) {
        out.accept(context, in.toInstant().atZone(getZoneId()));
    }
}

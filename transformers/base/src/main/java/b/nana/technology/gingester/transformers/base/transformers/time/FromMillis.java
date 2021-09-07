package b.nana.technology.gingester.transformers.base.transformers.time;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;

public class FromMillis extends InstantBase<Long> {

    public FromMillis(Parameters parameters) {
        super(parameters);
    }

    @Override
    public void transform(Context context, Long in, Receiver<TemporalAccessor> out) throws Exception {
        out.accept(context, Instant.ofEpochMilli(in).atZone(getZoneId()));
    }
}

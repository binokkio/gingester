package b.nana.technology.gingester.transformers.base.transformers.time;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;

public final class FromSeconds extends InstantBase<Long> {

    public FromSeconds(Parameters parameters) {
        super(parameters);
    }

    @Override
    public void transform(Context context, Long in, Receiver<TemporalAccessor> out) throws Exception {
        out.accept(context, Instant.ofEpochSecond(in).atZone(getZoneId()));
    }
}

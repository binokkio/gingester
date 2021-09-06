package b.nana.technology.gingester.transformers.base.transformers.time;

import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.receiver.Receiver;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;

public class Now extends InstantBase<Object> {

    public Now(Parameters parameters) {
        super(parameters);
    }

    @Override
    public void transform(Context context, Object in, Receiver<TemporalAccessor> out) throws Exception {
        out.accept(context, Instant.now().atZone(getZoneId()));
    }
}

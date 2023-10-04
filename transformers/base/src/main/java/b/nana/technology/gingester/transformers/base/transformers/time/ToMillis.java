package b.nana.technology.gingester.transformers.base.transformers.time;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;

public final class ToMillis implements Transformer<TemporalAccessor, Long> {

    @Override
    public void transform(Context context, TemporalAccessor in, Receiver<Long> out) {
        out.accept(context, Instant.from(in).toEpochMilli());
    }
}

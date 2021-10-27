package b.nana.technology.gingester.transformers.base.transformers.time;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;

public class AsOffsetDateTime implements Transformer<TemporalAccessor, OffsetDateTime> {

    @Override
    public void transform(Context context, TemporalAccessor in, Receiver<OffsetDateTime> out) throws Exception {
        out.accept(context, OffsetDateTime.from(in));
    }
}

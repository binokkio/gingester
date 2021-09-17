package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.transformers.base.transformers.time.InstantBase;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.temporal.TemporalAccessor;

public final class LastModified extends InstantBase<Path> {

    public LastModified(InstantBase.Parameters parameters) {
        super(parameters);
    }

    @Override
    public void transform(Context context, Path in, Receiver<TemporalAccessor> out) throws Exception {
        out.accept(context, Files.getLastModifiedTime(in).toInstant().atZone(getZoneId()));
    }
}

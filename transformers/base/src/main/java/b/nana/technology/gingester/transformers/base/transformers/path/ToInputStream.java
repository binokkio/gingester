package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ToInputStream extends Transformer<Path, InputStream> {

    @Override
    protected void setup(Setup setup) {
        setup.syncOutputs();
    }

    @Override
    protected void transform(Context context, Path input) throws IOException {
        emit(context, Files.newInputStream(input));
    }
}

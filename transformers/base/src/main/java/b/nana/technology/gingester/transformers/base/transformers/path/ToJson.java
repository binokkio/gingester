package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.transformers.base.common.ToJsonBase;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ToJson extends ToJsonBase<Path> {

    public ToJson(Parameters parameters) {
        super(parameters);
    }

    @Override
    protected InputStream toInputStream(Path input) throws IOException {
        return Files.newInputStream(input);
    }
}

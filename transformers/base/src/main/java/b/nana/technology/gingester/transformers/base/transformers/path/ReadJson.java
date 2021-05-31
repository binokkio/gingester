package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.transformers.base.common.ToJsonBase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ReadJson extends ToJsonBase<Path> {

    public ReadJson(Parameters parameters) {
        super(parameters);
    }

    @Override
    protected InputStream toInputStream(Path input) throws IOException {
        return new ByteArrayInputStream(Files.readAllBytes(input));
    }
}

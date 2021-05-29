package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.transformers.base.common.FormatBase;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Format extends FormatBase<Path> {

    public Format(Parameters parameters) {
        super(parameters);
    }

    @Override
    protected Path getResult(String string) {
        return Paths.get(string);
    }
}

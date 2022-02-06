package b.nana.technology.gingester.transformers.base.transformers.path;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.nio.file.StandardOpenOption;

public final class Overwrite extends Write {

    public Overwrite(Parameters parameters) {
        super(parameters);
    }

    public static class Parameters extends Write.Parameters {

        @JsonCreator
        public Parameters() {
            openOptions = new StandardOpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING };
        }

        @JsonCreator
        public Parameters(String path) {
            this.path = path;
            this.openOptions = new StandardOpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING };
        }
    }
}

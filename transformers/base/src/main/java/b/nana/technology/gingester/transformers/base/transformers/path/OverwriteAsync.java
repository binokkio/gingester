package b.nana.technology.gingester.transformers.base.transformers.path;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.nio.file.StandardOpenOption;

public final class OverwriteAsync extends WriteAsync {

    public OverwriteAsync(Parameters parameters) {
        super(parameters);
    }

    public static class Parameters extends WriteAsync.Parameters {

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
package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.nio.file.StandardOpenOption;

public final class Overwrite extends Write {

    public Overwrite(Parameters parameters) {
        super(parameters);
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters extends Write.Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, path -> o("path", path));
            }
        }

        public Parameters() {
            openOptions = new StandardOpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING };
        }
    }
}

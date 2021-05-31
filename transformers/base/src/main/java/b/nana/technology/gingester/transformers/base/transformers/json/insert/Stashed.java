package b.nana.technology.gingester.transformers.base.transformers.json.insert;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.transformers.base.common.json.insert.InsertBase;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Stashed extends InsertBase {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String[] jsonPath;
    private final String[] name;

    public Stashed(Parameters parameters) {
        super(parameters);
        jsonPath = parameters.jsonPath;
        name = parameters.name;
    }

    @Override
    protected void transform(Context context, JsonNode input) {

        context.fetch(name).ifPresent(object ->
                prepare(input, jsonPath)
                        .set(jsonPath[jsonPath.length - 1], objectMapper.valueToTree(object)));

        emit(context, input);
    }

    public static class Parameters extends InsertBase.Parameters {

        public String[] jsonPath;
        public String[] name;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String name) {
            String[] parts = name.split("\\.");
            this.jsonPath = parts;
            this.name = parts;
        }
    }
}

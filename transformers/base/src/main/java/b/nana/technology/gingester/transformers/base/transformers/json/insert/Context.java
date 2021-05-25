package b.nana.technology.gingester.transformers.base.transformers.json.insert;

import b.nana.technology.gingester.transformers.base.common.json.insert.InsertBase;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Context extends InsertBase {

    private final String[] jsonPath;

    public Context() {
        this(new Parameters());
    }

    public Context(Parameters parameters) {
        super(parameters);
        jsonPath = parameters.jsonPath;
    }

    @Override
    protected void transform(b.nana.technology.gingester.core.Context context, JsonNode input) {
        prepare(input, jsonPath).put(jsonPath[jsonPath.length - 1], context.getDescription());
        emit(context, input);
    }

    public static class Parameters extends InsertBase.Parameters {

        public String[] jsonPath = new String[] { "context" };

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String jsonPath) {
            this.jsonPath = jsonPath.split("\\.");
        }
    }
}

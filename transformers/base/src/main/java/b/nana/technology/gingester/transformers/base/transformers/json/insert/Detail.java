package b.nana.technology.gingester.transformers.base.transformers.json.insert;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.transformers.base.common.json.insert.InsertBase;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class Detail extends InsertBase {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String[] jsonPath;
    private final String[] detailName;

    public Detail(Parameters parameters) {
        super(parameters);
        jsonPath = parameters.jsonPath;
        detailName = parameters.detailName;
    }

    @Override
    protected void transform(Context context, JsonNode input) {

        context.getDetail(detailName).ifPresent(detail -> {
            prepare(input, jsonPath).set(jsonPath[jsonPath.length - 1], objectMapper.valueToTree(detail));
        });

        emit(context, input);
    }

    public static class Parameters extends InsertBase.Parameters {

        public String[] jsonPath;
        public String[] detailName;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String detailName) {
            String[] parts = detailName.split("\\.");
            this.jsonPath = parts;
            this.detailName = parts;
        }
    }
}

package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class ToString extends Transformer<JsonNode, String> {

    private final ObjectWriter objectWriter;

    public ToString(Parameters parameters) {
        super(parameters);
        ObjectMapper objectMapper = new ObjectMapper();
        objectWriter = parameters.pretty ?
                objectMapper.writerWithDefaultPrettyPrinter() :
                objectMapper.writer();
    }


    @Override
    protected void transform(Context context, JsonNode input) throws Exception {
        emit(context, objectWriter.writeValueAsString(input));
    }

    public static class Parameters {

        public boolean pretty;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(boolean pretty) {
            this.pretty = pretty;
        }
    }
}

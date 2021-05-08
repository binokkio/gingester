package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ToJson extends Transformer<String, JsonNode> {

    private final ObjectMapper objectMapper = new ObjectMapper();  // TODO allow configuration options to be passed in through Parameters

    @Override
    protected void transform(Context context, String input) throws JsonProcessingException {
        emit(context, objectMapper.readTree(input));
    }
}

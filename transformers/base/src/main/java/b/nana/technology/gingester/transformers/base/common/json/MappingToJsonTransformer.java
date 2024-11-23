package b.nana.technology.gingester.transformers.base.common.json;

import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class MappingToJsonTransformer<I, O extends JsonNode> implements Transformer<I, O> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected <T extends JsonNode> T valueToTree(Object value) {
        return objectMapper.valueToTree(value);
    }
}

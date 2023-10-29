package b.nana.technology.gingester.transformers.jdbc;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Iterator;
import java.util.Map;

public final class LoadJson extends Load<JsonNode, JsonNode> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final JavaType OBJECT_TYPE = OBJECT_MAPPER.constructType(Object.class);

    public LoadJson(Parameters parameters) {
        super(parameters);
    }

    @Override
    protected Iterator<Map.Entry<String, JsonNode>> getFields(JsonNode in) {
        return in.fields();
    }

    @Override
    protected boolean isNull(JsonNode value) {
        return value.isNull();
    }

    @Override
    protected String getSqlType(String key, JsonNode value) {

        // TODO support per-key overrides in parameters

        if (value.isIntegralNumber()) {
            return "BIGINT";
        } else if (value.isNumber()) {
            return "REAL";
        } else if (value.isTextual()) {
            return "TEXT";
        } else if (value.isBoolean()) {
            return "BOOLEAN";
        }

        throw new IllegalArgumentException("No sql type mapping for " + value.getNodeType().name());
    }

    @Override
    protected Object convert(JsonNode value) {
        return OBJECT_MAPPER.convertValue(value, OBJECT_TYPE);
    }
}

package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.annotations.Experimental;

import java.util.Iterator;
import java.util.Map;

@Experimental
public final class LoadMap extends Load<Map<String, Object>, Object> {

    public LoadMap(Parameters parameters) {
        super(parameters);
    }

    @Override
    protected Iterator<Map.Entry<String, Object>> getFields(Map<String, Object> in) {
        return in.entrySet().iterator();
    }

    @Override
    protected boolean isNull(Object value) {
        return value == null;
    }

    @Override
    protected String getSqlType(String key, Object value) {

        // TODO support per-key overrides in parameters

        if (value instanceof Long) {
            return "BIGINT";
        } else if (value instanceof Double) {
            return "REAL";
        } else if (value instanceof String) {
            return "TEXT";
        } else if (value instanceof Boolean) {
            return "BOOLEAN";
        }

        throw new IllegalArgumentException("No sql type mapping for " + value.getClass().getCanonicalName());
    }

    @Override
    protected Object convert(Object value) {
        return value;
    }
}

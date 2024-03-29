package b.nana.technology.gingester.transformers.jdbc.statement;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.FetchKey;
import b.nana.technology.gingester.transformers.jdbc.JdbcTransformer;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class Parameter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final JavaType OBJECT_TYPE = OBJECT_MAPPER.constructType(Object.class);

    private final PreparedStatement preparedStatement;

    private final int index;
    private final FetchKey fetchValue;

    public Parameter(PreparedStatement preparedStatement, int index, JdbcTransformer.Parameters.Statement.Parameter parameter) {
        this.preparedStatement = preparedStatement;
        this.index = index;
        this.fetchValue = new FetchKey(parameter.stash);
    }

    void update(Context context) throws SQLException {
        Object value = context.fetch(fetchValue).orElse(null);
        if (value instanceof JsonNode) {
            preparedStatement.setObject(index, OBJECT_MAPPER.convertValue(value, OBJECT_TYPE));
        } else {
            preparedStatement.setObject(index, value);
        }
    }
}

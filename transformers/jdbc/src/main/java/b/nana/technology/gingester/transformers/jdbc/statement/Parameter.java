package b.nana.technology.gingester.transformers.jdbc.statement;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.transformers.Fetch;
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
    private final String[] stash;
//    private final String type;
//    private final JsonNode instructions;

    public Parameter(PreparedStatement preparedStatement, int index, JdbcTransformer.Parameters.Statement.Parameter parameter) throws SQLException {
        this.preparedStatement = preparedStatement;
        this.index = index;
        this.stash = Fetch.parseStashName(parameter.stash);
//        this.type = preparedStatement.getParameterMetaData().getParameterTypeName(index);
//        this.instructions = parameter.instructions;
    }

    void update(Context context) throws SQLException {
        Object value = context.fetch(stash).findFirst().orElseThrow();
        if (value instanceof JsonNode) {
            preparedStatement.setObject(index, OBJECT_MAPPER.convertValue(value, OBJECT_TYPE));
        } else {
            preparedStatement.setObject(index, value);
        }
    }
}

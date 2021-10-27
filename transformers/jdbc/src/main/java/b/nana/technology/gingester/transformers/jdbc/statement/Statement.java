package b.nana.technology.gingester.transformers.jdbc.statement;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.transformers.jdbc.JdbcTransformer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class Statement {

    private final PreparedStatement preparedStatement;
    private final List<Parameter> parameters;

    public Statement(Connection connection, JdbcTransformer.Parameters.Statement statement) throws SQLException {
        preparedStatement = connection.prepareStatement(statement.sql);
        this.parameters = new ArrayList<>();
        for (int i = 0; i < statement.parameters.size(); i++) {
            this.parameters.add(new Parameter(preparedStatement, i + 1, statement.parameters.get(i)));
        }
    }

    protected void updateParameters(Context context) throws SQLException {
        for (Parameter parameter : parameters) {
            parameter.update(context);
        }
    }

    public PreparedStatement getPreparedStatement() {
        return preparedStatement;
    }
}

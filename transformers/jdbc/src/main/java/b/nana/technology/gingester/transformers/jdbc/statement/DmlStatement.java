package b.nana.technology.gingester.transformers.jdbc.statement;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.transformers.jdbc.JdbcTransformer;

import java.sql.Connection;
import java.sql.SQLException;

public final class DmlStatement extends Statement {

    public DmlStatement(Connection connection, JdbcTransformer.Parameters.Statement statement) throws SQLException {
        super(connection, statement);
    }

    public int execute(Context context) throws SQLException {
        updateParameters(context);
        return getPreparedStatement().executeUpdate();
    }
}

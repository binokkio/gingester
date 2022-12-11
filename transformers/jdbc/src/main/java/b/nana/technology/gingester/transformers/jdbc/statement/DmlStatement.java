package b.nana.technology.gingester.transformers.jdbc.statement;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.transformers.jdbc.JdbcTransformer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public final class DmlStatement extends Statement {

    public DmlStatement(Connection connection, String statement, List<JdbcTransformer.Parameters.Statement.Parameter> parameters, boolean returnGeneratedKeys) throws SQLException {
        super(connection, statement, parameters, returnGeneratedKeys);
    }

    public int execute(Context context) throws SQLException {
        updateParameters(context);
        return getPreparedStatement().executeUpdate();
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        return getPreparedStatement().getGeneratedKeys();
    }
}

package b.nana.technology.gingester.transformers.jdbc.statement;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.transformers.jdbc.JdbcTransformer;
import b.nana.technology.gingester.transformers.jdbc.result.FlatResultStructure;
import b.nana.technology.gingester.transformers.jdbc.result.ResultStructure;
import b.nana.technology.gingester.transformers.jdbc.result.TabledResultStructure;

import java.sql.*;
import java.util.List;
import java.util.Map;

public final class DqlStatement extends Statement {

    private final ResultStructure resultStructure;
    private final boolean[] booleans;

    public DqlStatement(Connection connection, String statement, List<JdbcTransformer.Parameters.Statement.Parameter> parameters, Integer fetchSize, boolean columnsOnly) throws SQLException {
        super(connection, statement, parameters);

        PreparedStatement preparedStatement = getPreparedStatement();
        if (fetchSize != null)
            preparedStatement.setFetchSize(fetchSize);

        ResultSetMetaData resultSetMetaData = preparedStatement.getMetaData();

        resultStructure = columnsOnly ?
                new FlatResultStructure(this) :
                new TabledResultStructure(this);

        booleans = new boolean[resultSetMetaData.getColumnCount() + 1];
        for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
            if (resultSetMetaData.getColumnTypeName(i).equals("BOOLEAN")) {
                booleans[i] = true;
            }
        }
    }

    public ResultSet execute(Context context) throws SQLException {
        updateParameters(context);
        return getPreparedStatement().executeQuery();
    }

    public Map<String, Object> readRow(ResultSet resultSet) {
        return resultStructure.readRow(resultSet);
    }

    public Object getColumnValue(ResultSet resultSet, int i) {
        try {
            return booleans[i] ? resultSet.getBoolean(i) : resultSet.getObject(i);
        } catch (SQLException e) {
            try {
                return resultSet.getString(i);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);  // TODO
            }
        }
    }
}

package b.nana.technology.gingester.transformers.jdbc.statement;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.transformers.jdbc.JdbcTransformer;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DqlStatement extends Statement {

    private final ResultSetMetaData resultSetMetaData;
    private final Map<String, Map<Integer, String>> resultStructure;

    public DqlStatement(Connection connection, String statement, List<JdbcTransformer.Parameters.Statement.Parameter> parameters) throws SQLException {
        this(connection, statement, parameters, null);
    }

    public DqlStatement(Connection connection, String statement, List<JdbcTransformer.Parameters.Statement.Parameter> parameters, Integer fetchSize) throws SQLException {
        super(connection, statement, parameters);

        PreparedStatement preparedStatement = getPreparedStatement();
        if (fetchSize != null) {
            preparedStatement.setFetchSize(fetchSize);
        }

        resultSetMetaData = preparedStatement.getMetaData();
        resultStructure = new HashMap<>();

        for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {

            String tableName = resultSetMetaData.getTableName(i);
            String columnName = resultSetMetaData.getColumnName(i);

            if (columnName.indexOf('.') > 0) {
                String[] parts = columnName.split("\\.", 2);
                tableName = parts[0];
                columnName = parts[1];
            } else if (tableName.isEmpty()) {
                tableName = "__calculated__";
            }

            String collision = resultStructure
                    .computeIfAbsent(tableName, x -> new HashMap<>())
                    .put(i, columnName);

            if (collision != null) {
                throw new IllegalArgumentException("Multiple columns map to " + tableName + "." + columnName);
            }
        }
    }

    public ResultSet execute(Context context) throws SQLException {
        updateParameters(context);
        return getPreparedStatement().executeQuery();
    }

    public Map<String, Map<Integer, String>> getResultStructure() {
        return resultStructure;
    }

    public Object getColumnValue(ResultSet resultSet, int i) {
        try {
            return resultSetMetaData.getColumnTypeName(i).equals("BOOLEAN") ? resultSet.getBoolean(i) : resultSet.getObject(i);
        } catch (SQLException e) {
            try {
                return resultSet.getString(i);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);  // TODO
            }
        }
    }
}

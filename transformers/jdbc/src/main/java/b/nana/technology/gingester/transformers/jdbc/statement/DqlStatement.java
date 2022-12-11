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

    public DqlStatement(Connection connection, String statement, List<JdbcTransformer.Parameters.Statement.Parameter> parameters, Integer fetchSize, boolean columnsOnly) throws SQLException {
        super(connection, statement, parameters, false);

        PreparedStatement preparedStatement = getPreparedStatement();
        if (fetchSize != null)
            preparedStatement.setFetchSize(fetchSize);

        ResultSetMetaData resultSetMetaData = preparedStatement.getMetaData();

        resultStructure = columnsOnly ?
                new FlatResultStructure(resultSetMetaData) :
                new TabledResultStructure(resultSetMetaData);
    }

    public ResultSet execute(Context context) throws SQLException {
        updateParameters(context);
        return getPreparedStatement().executeQuery();
    }

    public Map<String, Object> readRow(ResultSet resultSet) {
        return resultStructure.readRow(resultSet);
    }
}

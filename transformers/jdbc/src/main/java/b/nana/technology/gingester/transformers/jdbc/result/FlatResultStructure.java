package b.nana.technology.gingester.transformers.jdbc.result;

import b.nana.technology.gingester.transformers.jdbc.statement.DqlStatement;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public final class FlatResultStructure implements ResultStructure {

    private final DqlStatement dqlStatement;
    private final Map<Integer, String> resultStructure;

    public FlatResultStructure(DqlStatement dqlStatement) throws SQLException {

        this.dqlStatement = dqlStatement;
        ResultSetMetaData resultSetMetaData = dqlStatement.getPreparedStatement().getMetaData();
        resultStructure = new HashMap<>();

        for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {

            String columnName = resultSetMetaData.getColumnName(i);
            String collision = resultStructure.put(i, columnName);

            if (collision != null)
                throw new IllegalArgumentException("Multiple columns map to " + columnName);
        }
    }

    @Override
    public Map<String, Object> readRow(ResultSet resultSet) {
        Map<String, Object> result = new HashMap<>();  // TODO allow map implementation to be specified (hash, link, tree)
        resultStructure.forEach((index, name) -> result.put(name, dqlStatement.getColumnValue(resultSet, index)));
        return result;
    }
}

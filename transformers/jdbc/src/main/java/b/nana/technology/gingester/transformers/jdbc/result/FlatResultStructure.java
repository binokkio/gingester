package b.nana.technology.gingester.transformers.jdbc.result;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FlatResultStructure extends ResultStructure {

    private final Map<Integer, String> resultStructure;

    public FlatResultStructure(ResultSetMetaData resultSetMetaData) throws SQLException {
        super(resultSetMetaData);

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
        Map<String, Object> result = new LinkedHashMap<>();  // TODO allow map implementation to be specified (hash, link, tree)
        resultStructure.forEach((index, name) -> result.put(name, getColumnValue(resultSet, index)));
        return result;
    }
}

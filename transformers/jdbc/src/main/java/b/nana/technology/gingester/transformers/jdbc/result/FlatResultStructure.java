package b.nana.technology.gingester.transformers.jdbc.result;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
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
    public void readRowInto(ResultSet resultSet, Map<String, Object> destination) {
        resultStructure.forEach((index, name) -> destination.put(name, getColumnValue(resultSet, index)));
    }
}

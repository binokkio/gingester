package b.nana.technology.gingester.transformers.jdbc.result;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TabledResultStructure extends ResultStructure {

    private final Map<String, Map<Integer, String>> resultStructure;

    public TabledResultStructure(ResultSetMetaData resultSetMetaData) throws SQLException {
        super(resultSetMetaData);

        resultStructure = new LinkedHashMap<>();

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

            if (collision != null)
                throw new IllegalArgumentException("Multiple columns map to " + tableName + "." + columnName);
        }
    }

    @Override
    public void readRowInto(ResultSet resultSet, Map<String, Object> destination) {
        resultStructure.forEach((tableName, columns) -> {
            Map<String, Object> table = new LinkedHashMap<>();  // TODO allow map implementation to be specified (hash, link, tree)
            destination.put(tableName, table);
            columns.forEach((index, name) -> table.put(name, getColumnValue(resultSet, index)));
        });
    }
}

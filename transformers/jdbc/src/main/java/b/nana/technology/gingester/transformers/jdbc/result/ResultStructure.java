package b.nana.technology.gingester.transformers.jdbc.result;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

public abstract class ResultStructure {

    private final boolean[] booleans;

    ResultStructure(ResultSetMetaData resultSetMetaData) throws SQLException {
        booleans = new boolean[resultSetMetaData.getColumnCount() + 1];
        for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
            if (resultSetMetaData.getColumnTypeName(i).equals("BOOLEAN")) {
                booleans[i] = true;
            }
        }
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

    public abstract Map<String, Object> readRow(ResultSet resultSet);
}

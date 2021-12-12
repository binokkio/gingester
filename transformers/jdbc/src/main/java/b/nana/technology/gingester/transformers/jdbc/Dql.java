package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.transformers.jdbc.statement.DqlStatement;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public final class Dql extends JdbcTransformer<Object, Map<String, Map<String, ?>>> {

    private final JdbcTransformer.Parameters.Statement dql;

    private DqlStatement dqlStatement;
    private ResultSetMetaData resultSetMetaData;
    private Map<String, Map<Integer, String>> resultStructure;

    public Dql(Parameters parameters) {
        super(parameters);
        dql = parameters.dql;
    }

    @Override
    public void open() throws Exception {
        super.open();

        getDdlExecuted().awaitAdvance(0);

        dqlStatement = new DqlStatement(getConnection(), dql);
        resultSetMetaData = dqlStatement.getPreparedStatement().getMetaData();

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

    @Override
    public void transform(Context context, Object in, Receiver<Map<String, Map<String, ?>>> out) throws Exception {
        try (ResultSet resultSet = dqlStatement.execute(context)) {
            for (long i = 0; resultSet.next(); i++) {
                Map<String, Map<String, ?>> result = new HashMap<>();
                resultStructure.forEach((tableName, columns) -> {
                    Map<String, Object> table = new HashMap<>();
                    result.put(tableName, table);
                    columns.forEach((index, name) -> table.put(name, getColumnValue(resultSet, index)));
                });
                out.accept(context.stash("description", dql.statement + " :: " + i), result);
            }
        }
    }

    private Object getColumnValue(ResultSet resultSet, int i) {
        try {
            return resultSetMetaData.getColumnTypeName(i).equals("BOOLEAN") ? resultSet.getBoolean(i) : resultSet.getObject(i);
        } catch (SQLException e) {
            throw new RuntimeException(e);  // TODO
        }
    }

    public static class Parameters extends JdbcTransformer.Parameters {

        public Statement dql;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String dql) {
            this.dql = new Statement(dql);
        }
    }
}

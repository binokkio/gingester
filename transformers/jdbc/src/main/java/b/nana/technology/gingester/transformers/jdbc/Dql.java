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

public final class Dql extends JdbcTransformer<Object, Map<String, Object>> {

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
            Map<Integer, String> resultTable = resultStructure.computeIfAbsent(resultSetMetaData.getTableName(i), x -> new HashMap<>());
            resultTable.put(i, resultSetMetaData.getColumnName(i));
        }
    }

    @Override
    public void transform(Context context, Object in, Receiver<Map<String, Object>> out) throws Exception {
        try (ResultSet resultSet = dqlStatement.execute(context)) {
            while (resultSet.next()) {
                Map<String, Object> result = new HashMap<>();
                resultStructure.forEach((tableName, columns) -> {
                    Map<String, Object> container;
                    if (tableName.isEmpty()) {
                        container = result;
                    } else {
                        container = new HashMap<>();
                        result.put(tableName, container);
                    }
                    columns.forEach((index, name) -> container.put(name, getColumnValue(resultSet, index)));
                });
                out.accept(context, result);
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

package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.transformers.jdbc.statement.DqlStatement;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.Map;

public final class Dql extends JdbcTransformer<Object, Map<String, Object>> {

    private final JdbcTransformer.Parameters.Statement dql;

    private DqlStatement dqlStatement;

    public Dql(Parameters parameters) {
        super(parameters);
        dql = parameters.dql;
    }

    @Override
    public void open() throws Exception {
        super.open();
        getDdlExecuted().awaitAdvance(0);
        dqlStatement = new DqlStatement(getConnection(), dql);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Map<String, Object>> out) throws Exception {
        try (ResultSet resultSet = dqlStatement.execute(context)) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    row.put(metaData.getColumnName(i), metaData.getColumnTypeName(i).equals("BOOLEAN") ? resultSet.getBoolean(i) : resultSet.getObject(i));
                }
                out.accept(context, row);
            }
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

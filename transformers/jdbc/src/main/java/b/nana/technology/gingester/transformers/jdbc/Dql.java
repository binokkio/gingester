package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.transformers.jdbc.statement.DqlStatement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.sql.ResultSet;
import java.util.Map;

public final class Dql extends JdbcTransformer<Object, Map<String, Object>, DqlStatement> {

    private final JdbcTransformer.Parameters.Statement dql;
    private final Template dqlTemplate;
    private final Integer fetchSize;
    private final boolean columnsOnly;

    public Dql(Parameters parameters) {
        super(parameters, true);
        dql = parameters.dql;
        dqlTemplate = Context.newTemplate(dql.statement);
        fetchSize = parameters.fetchSize;
        columnsOnly = parameters.columnsOnly;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Map<String, Object>> out) throws Exception {

        ConnectionWith<DqlStatement> connection = acquireConnection(context, in);
        try {

            String raw;
            DqlStatement tempDqlStatement;

            if (dqlTemplate.isInvariant()) {
                raw = dqlTemplate.requireInvariant();
                tempDqlStatement = connection.getSingleton();
                if (tempDqlStatement == null) {
                    tempDqlStatement = new DqlStatement(connection.getConnection(), raw, dql.parameters, fetchSize, columnsOnly);
                    connection.setSingleton(tempDqlStatement);
                }
            } else {
                raw = dqlTemplate.render(context, in);
                tempDqlStatement = connection.getObject(raw);
                if (tempDqlStatement == null) {
                    tempDqlStatement = new DqlStatement(connection.getConnection(), raw, dql.parameters, fetchSize, columnsOnly);
                    DqlStatement removed = connection.setObject(raw, tempDqlStatement);
                    if (removed != null) removed.close();
                }
            }

            final DqlStatement dqlStatement = tempDqlStatement;

            try (ResultSet resultSet = dqlStatement.execute(context)) {
                for (long i = 0; resultSet.next(); i++) {
                    out.accept(
                            context.stash("description", raw + " :: " + i),
                            dqlStatement.readRow(resultSet)
                    );
                }
            }

        } finally {
            releaseConnection(context, connection);
        }
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters extends JdbcTransformer.Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, text -> o("dql", text));
                rule(JsonNode::isArray, array -> flags(array, 1, o("dql", array.get(0))));
            }
        }

        public Statement dql;
        public Integer fetchSize;
        public boolean columnsOnly;
    }
}

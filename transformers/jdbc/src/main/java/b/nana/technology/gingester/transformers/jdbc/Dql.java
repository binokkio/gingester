package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.transformers.jdbc.statement.DqlStatement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public final class Dql extends JdbcTransformer<Object, Map<String, Map<String, ?>>, DqlStatement> {

    private final JdbcTransformer.Parameters.Statement dql;
    private final Template dqlTemplate;
    private final Integer fetchSize;

    public Dql(Parameters parameters) {
        super(parameters, true);
        dql = parameters.dql;
        dqlTemplate = Context.newTemplate(dql.statement);
        fetchSize = parameters.fetchSize;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Map<String, Map<String, ?>>> out) throws Exception {

        ConnectionWith<DqlStatement> connection = acquireConnection(context);
        try {

            DqlStatement tempDqlStatement;

            if (dqlTemplate.isInvariant()) {
                tempDqlStatement = connection.getSingleton();
                if (tempDqlStatement == null) {
                    tempDqlStatement = new DqlStatement(connection.getConnection(), dqlTemplate.requireInvariant(), dql.parameters, fetchSize);
                    connection.setSingleton(tempDqlStatement);
                }
            } else {
                String raw = dqlTemplate.render(context);
                tempDqlStatement = connection.getObject(raw);
                if (tempDqlStatement == null) {
                    tempDqlStatement = new DqlStatement(connection.getConnection(), raw, dql.parameters, fetchSize);
                    DqlStatement removed = connection.setObject(raw, tempDqlStatement);
                    if (removed != null) removed.close();
                }
            }

            final DqlStatement dqlStatement = tempDqlStatement;

            try (ResultSet resultSet = dqlStatement.execute(context)) {
                for (long i = 0; resultSet.next(); i++) {
                    Map<String, Map<String, ?>> result = new HashMap<>();  // TODO allow map implementation to be specified (hash, link, tree)
                    dqlStatement.getResultStructure().forEach((tableName, columns) -> {
                        Map<String, Object> table = new HashMap<>();  // TODO allow map implementation to be specified (hash, link, tree)
                        result.put(tableName, table);
                        columns.forEach((index, name) -> table.put(name, dqlStatement.getColumnValue(resultSet, index)));
                    });
                    out.accept(context.stash("description", dql.statement + " :: " + i), result);
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
            }
        }

        public Statement dql;
        public Integer fetchSize;
    }
}

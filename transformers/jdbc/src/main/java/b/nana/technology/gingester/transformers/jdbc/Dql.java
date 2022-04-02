package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.transformers.jdbc.statement.DqlStatement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public final class Dql extends JdbcTransformer<Object, Map<String, Map<String, ?>>> {

    private final JdbcTransformer.Parameters.Statement dql;
    private final Integer fetchSize;

    private TemplateMapper<DqlStatement> dqlStatementTemplate;

    public Dql(Parameters parameters) {
        super(parameters);
        dql = parameters.dql;
        fetchSize = parameters.fetchSize;
    }

    @Override
    public void open() throws Exception {
        super.open();
        getDdlExecuted().awaitAdvance(0);
        dqlStatementTemplate = Context.newTemplateMapper(dql.statement, s -> {
            if (fetchSize != null) {
                return new DqlStatement(getConnection(), s, dql.parameters, fetchSize);
            } else {
                return new DqlStatement(getConnection(), s, dql.parameters);
            }
        });
    }

    @Override
    public void transform(Context context, Object in, Receiver<Map<String, Map<String, ?>>> out) throws Exception {
        DqlStatement dqlStatement = dqlStatementTemplate.render(context);
        try (ResultSet resultSet = dqlStatement.execute(context)) {
            for (long i = 0; resultSet.next(); i++) {
                Map<String, Map<String, ?>> result = new HashMap<>();
                dqlStatement.getResultStructure().forEach((tableName, columns) -> {
                    Map<String, Object> table = new HashMap<>();
                    result.put(tableName, table);
                    columns.forEach((index, name) -> table.put(name, dqlStatement.getColumnValue(resultSet, index)));
                });
                out.accept(context.stash("description", dql.statement + " :: " + i), result);
            }
        } finally {
            if (!dqlStatementTemplate.isInvariant()) {
                dqlStatement.close();
            }
        }
    }

    @Override
    public void close() throws Exception {
        super.close();
        if (dqlStatementTemplate.isInvariant()) {
            dqlStatementTemplate.requireInvariant().close();
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

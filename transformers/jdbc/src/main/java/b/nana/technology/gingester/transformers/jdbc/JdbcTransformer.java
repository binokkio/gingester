package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

public abstract class JdbcTransformer<I, O, T> implements Transformer<I, O> {

    private final Template urlTemplate;
    private final MixedConnectionsPool<T> mixedConnectionsPool;

    public JdbcTransformer(Parameters parameters, boolean autoCommit) {
        urlTemplate = Context.newTemplate(parameters.url);
        mixedConnectionsPool = new MixedConnectionsPool<>(
                parameters.connectionPoolSize,
                parameters.statementPoolSize,
                connectionWith -> {

                    Connection connection = connectionWith.getConnection();

                    if (parameters.ddl.isEmpty()) {
                        if (!autoCommit) {
                            connection.setAutoCommit(false);
                        }
                    } else {

                        connection.setAutoCommit(false);

                        try {
                            for (String statement : parameters.ddl) {
                                try (Statement s = connection.createStatement()) {
                                    s.execute(statement);
                                }
                            }
                            connection.commit();
                        } catch (SQLException e) {
                            connection.rollback();
                            throw e;
                        }

                        if (autoCommit) {
                            connection.setAutoCommit(true);
                        }
                    }
                },
                this::onConnectionMoribund);
    }

    protected final ConnectionWith<T> acquireConnection(Context context) throws SQLException, InterruptedException {
        return mixedConnectionsPool.acquire(urlTemplate.render(context));
    }

    protected final void releaseConnection(ConnectionWith<T> connection) {
        mixedConnectionsPool.release(connection);
    }

    protected void onConnectionMoribund(ConnectionWith<T> connectionWith) throws SQLException {

    }

    @Override
    public void close() throws Exception {
        mixedConnectionsPool.close();
    }

    public static class Parameters {

        public TemplateParameters url = new TemplateParameters("jdbc:sqlite:file::memory:?cache=shared", true);
        public List<String> ddl = Collections.emptyList();
        public int connectionPoolSize = 10;
        public int statementPoolSize = 100;

        @JsonDeserialize(using = Statement.Deserializer.class)
        public static class Statement {

            public static class Deserializer extends NormalizingDeserializer<Statement> {
                public Deserializer() {
                    super(Statement.class);
                    rule(JsonNode::isTextual, text -> o("statement", text));
                    rule(json -> json.has("template"), json -> o("statement", json));
                }
            }

            public TemplateParameters statement;
            public List<Parameter> parameters = Collections.emptyList();

            @JsonDeserialize(using = Parameter.Deserializer.class)
            public static class Parameter {

                public static class Deserializer extends NormalizingDeserializer<Parameter> {
                    public Deserializer() {
                        super(Parameter.class);
                        rule(JsonNode::isTextual, text -> o("stash", text));
                    }
                }

                public String stash;
                public JsonNode instructions;  // can be used to communicate e.g. date formats
            }
        }
    }
}

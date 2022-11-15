package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
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

    private final ContextMap<MixedConnectionsPool<T>> state = new ContextMap<>();

    private final Template urlTemplate;
    private final List<String> ddl;
    private final int connectionPoolSize;
    private final int statementPoolSize;
    private final boolean autoCommit;

    public JdbcTransformer(Parameters parameters, boolean autoCommit) {
        this.urlTemplate = Context.newTemplate(parameters.url);
        this.ddl = parameters.ddl;
        this.connectionPoolSize = parameters.connectionPoolSize;
        this.statementPoolSize = parameters.statementPoolSize;
        this.autoCommit = autoCommit;
    }

    @Override
    public void prepare(Context context, Receiver<O> out) {
        state.put(context, new MixedConnectionsPool<>(
                connectionPoolSize,
                statementPoolSize,
                connectionWith -> {

                    Connection connection = connectionWith.getConnection();

                    if (ddl.isEmpty()) {
                        if (!autoCommit) {
                            connection.setAutoCommit(false);
                        }
                    } else {

                        connection.setAutoCommit(false);

                        try {
                            for (String statement : ddl) {
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
                this::onConnectionMoribund));
    }

    protected final ConnectionWith<T> acquireConnection(Context context, Object in) throws SQLException, InterruptedException {
        return state.get(context).acquire(urlTemplate.render(context, in));
    }

    protected final void releaseConnection(Context context, ConnectionWith<T> connection) {
        state.get(context).release(connection);
    }

    protected void onConnectionMoribund(ConnectionWith<T> connectionWith) throws SQLException {

    }

    @Override
    public void finish(Context context, Receiver<O> out) throws Exception {
        state.remove(context).close();
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

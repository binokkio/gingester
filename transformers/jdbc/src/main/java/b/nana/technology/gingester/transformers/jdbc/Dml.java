package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.transformers.jdbc.statement.DmlStatement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public final class Dml extends JdbcTransformer<Object, Object, DmlStatement> {

    private final List<JdbcTransformer.Parameters.Statement> dml;
    private final List<Template> dmlTemplates;
    private final CommitMode commitMode;

    public Dml(Parameters parameters) {
        super(parameters, false);
        dml = parameters.dml;
        dmlTemplates = parameters.dml.stream().map(d -> d.statement).map(Context::newTemplate).collect(Collectors.toList());
        commitMode = parameters.commitMode;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws InterruptedException, SQLException {

        ConnectionWith<DmlStatement> connection = acquireConnection(context, in);
        try {

            for (int i = 0; i < dml.size(); i++) {

                Template dmlTemplate = dmlTemplates.get(i);
                DmlStatement dmlStatement;

                String raw = dmlTemplate.render(context, in);
                dmlStatement = connection.getObject(raw);
                if (dmlStatement == null) {
                    dmlStatement = new DmlStatement(connection.getConnection(), raw, dml.get(i).parameters);
                    DmlStatement removed = connection.setObject(raw, dmlStatement);
                    if (removed != null) removed.close();
                }

                dmlStatement.execute(context);

                if (commitMode == CommitMode.PER_STATEMENT) connection.getConnection().commit();
            }

            if (commitMode == CommitMode.PER_TRANSFORM) connection.getConnection().commit();

        } catch (SQLException e) {
            connection.getConnection().rollback();
            throw e;
        } finally {
            releaseConnection(context, connection);
        }

        out.accept(context, in);
    }

    @Override
    protected void onConnectionMoribund(ConnectionWith<DmlStatement> connection) throws SQLException {
        if (commitMode == CommitMode.PER_CONNECTION) {
            connection.getConnection().commit();
        }
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters extends JdbcTransformer.Parameters {

        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isTextual, text -> o("dml", text));
                rule(JsonNode::isArray, array -> o("dml", array));
                rule(json -> json.has("statement"), json -> o("dml", json));
            }
        }

        public List<JdbcTransformer.Parameters.Statement> dml;
        public CommitMode commitMode = CommitMode.PER_TRANSFORM;
    }

    public enum CommitMode {
        PER_STATEMENT,
        PER_TRANSFORM,
        PER_CONNECTION
    }
}

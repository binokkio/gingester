package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.transformers.jdbc.result.FlatResultStructure;
import b.nana.technology.gingester.transformers.jdbc.result.ResultStructure;
import b.nana.technology.gingester.transformers.jdbc.statement.DmlStatement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class Dml extends JdbcTransformer<Object, Object, DmlStatement> {

    private final List<Parameters.NamedStatement> dml;
    private final List<Template> dmlTemplates;
    private final ResultStructure[] resultStructures;
    private final CommitMode commitMode;
    private final boolean yieldGeneratedKeys;

    public Dml(Parameters parameters) {
        super(parameters, false);
        dml = parameters.dml;
        dmlTemplates = parameters.dml.stream().map(d -> d.statement).map(Context::newTemplate).collect(Collectors.toList());
        resultStructures = new FlatResultStructure[dml.size()];
        commitMode = parameters.commitMode;
        yieldGeneratedKeys = parameters.yieldGeneratedKeys;

        // TODO make duplicate dml.name values illegal
    }

    @Override
    public boolean isPassthrough() {
        return !yieldGeneratedKeys;
    }

    @Override
    public Object getOutputType() {
        // this method is only called if isPassthrough is false, in which case we always return a map
        return Map.class;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws InterruptedException, SQLException {

        Map<String, Object> result = yieldGeneratedKeys ? new LinkedHashMap<>() : null;

        ConnectionWith<DmlStatement> connection = acquireConnection(context, in);
        try {

            for (int i = 0; i < dml.size(); i++) {

                Parameters.NamedStatement namedStatement = dml.get(i);

                Template dmlTemplate = dmlTemplates.get(i);
                DmlStatement dmlStatement;

                String raw = dmlTemplate.render(context, in);
                dmlStatement = connection.getObject(raw);
                if (dmlStatement == null) {
                    dmlStatement = new DmlStatement(connection.getConnection(), raw, namedStatement.parameters, yieldGeneratedKeys);
                    DmlStatement removed = connection.setObject(raw, dmlStatement);
                    if (removed != null) removed.close();
                }

                dmlStatement.execute(context);

                if (commitMode == CommitMode.PER_STATEMENT)
                    connection.getConnection().commit();

                if (yieldGeneratedKeys && (namedStatement.name != null || dml.size() == 1)) {
                    try (ResultSet generatedKeys = dmlStatement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {  // NOTE that only the first row is taken into account here

                            // get/cache result structure
                            ResultStructure resultStructure = resultStructures[i];
                            if (resultStructure == null) {
                                resultStructure = new FlatResultStructure(generatedKeys.getMetaData());
                                resultStructures[i] = resultStructure;
                            }

                            if (dml.size() == 1) {
                                resultStructure.readRowInto(generatedKeys, result);
                            } else {
                                result.put(namedStatement.name, resultStructure.readRow(generatedKeys));
                            }
                        }
                    }
                }
            }

            if (commitMode == CommitMode.PER_TRANSFORM)
                connection.getConnection().commit();

        } catch (SQLException e) {
            connection.getConnection().rollback();
            throw e;
        } finally {
            releaseConnection(context, connection);
        }

        out.accept(context, yieldGeneratedKeys ? result : in);
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

        public List<NamedStatement> dml;
        public CommitMode commitMode = CommitMode.PER_TRANSFORM;
        public boolean yieldGeneratedKeys = true;

        @JsonDeserialize(using = NamedStatement.Deserializer.class)
        public static class NamedStatement extends Statement {
            public static class Deserializer extends NormalizingDeserializer<Parameters.NamedStatement> {
                public Deserializer() {
                    super(NamedStatement.class);
                    rule(JsonNode::isTextual, text -> o("statement", text));
                    rule(json -> json.has("template"), json -> o("statement", json));
                }
            }

            public String name;
        }
    }

    public enum CommitMode {
        PER_STATEMENT,
        PER_TRANSFORM,
        PER_CONNECTION
    }
}

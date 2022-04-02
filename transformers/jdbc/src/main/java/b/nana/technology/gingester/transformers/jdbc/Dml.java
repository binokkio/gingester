package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.transformers.jdbc.statement.DmlStatement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Dml extends JdbcTransformer<Object, Object> {

    private final List<JdbcTransformer.Parameters.Statement> dml;
    private final CommitMode commitMode;

    private List<TemplateMapper<DmlStatement>> dmlStatements;

    public Dml(Parameters parameters) {
        super(parameters);
        dml = parameters.dml;
        commitMode = parameters.commitMode;
    }

    @Override
    public void setup(SetupControls controls) {
        super.setup(controls);
        if (commitMode == CommitMode.PER_FINISH) {
            controls.syncs(Collections.singletonList("__seed__"));
        }
    }

    @Override
    public void open() throws Exception {
        super.open();
        getDdlExecuted().awaitAdvance(0);
        dmlStatements = new ArrayList<>();
        for (JdbcTransformer.Parameters.Statement statement : dml) {
            dmlStatements.add(Context.newTemplateMapper(statement.statement, s -> new DmlStatement(getConnection(), s, statement.parameters)));
        }
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws SQLException {

        try {
            for (TemplateMapper<DmlStatement> dmlStatementTemplate : dmlStatements) {
                DmlStatement dmlStatement = dmlStatementTemplate.render(context);
                try {
                    dmlStatement.execute(context);
                } finally {
                    if (!dmlStatementTemplate.isInvariant()) {
                        dmlStatement.close();
                    }
                }
                if (commitMode == CommitMode.PER_STATEMENT) getConnection().commit();
            }
            if (commitMode == CommitMode.PER_TRANSFORM) getConnection().commit();
        } catch (SQLException e) {
            getConnection().rollback();
            throw e;
        }

        out.accept(context, in);
    }

    @Override
    public void finish(Context context, Receiver<Object> out) throws Exception {
        if (commitMode == CommitMode.PER_FINISH) getConnection().commit();
    }

    @Override
    public void close() throws Exception {
        super.close();
        for (TemplateMapper<DmlStatement> dmlStatement : dmlStatements) {
            if (dmlStatement.isInvariant()) {
                dmlStatement.requireInvariant().close();
            }
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
        PER_FINISH
    }
}

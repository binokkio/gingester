package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.transformers.jdbc.statement.DmlStatement;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Dml extends JdbcTransformer<Object, Object> {

    private final List<JdbcTransformer.Parameters.Statement> dml;
    private final CommitMode commitMode;

    private List<DmlStatement> dmlStatements;

    public Dml(Parameters parameters) {
        super(parameters);
        dml = parameters.dml;
        commitMode = parameters.commitMode;
    }

    @Override
    public void setup(SetupControls controls) {
        if (commitMode == CommitMode.PER_FINISH) {
            controls.syncs(Collections.singletonList("__seed__"));
        }
    }

    @Override
    public void open() throws Exception {
        super.open();
        dmlStatements = new ArrayList<>();
        for (JdbcTransformer.Parameters.Statement statement : dml) {
            dmlStatements.add(new DmlStatement(getConnection(), statement));
        }
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws SQLException {

        try {
            for (DmlStatement dmlStatement : dmlStatements) {
                dmlStatement.execute(context);
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

    public static class Parameters extends JdbcTransformer.Parameters {

        public List<JdbcTransformer.Parameters.Statement> dml;
        public CommitMode commitMode = CommitMode.PER_TRANSFORM;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(List<JdbcTransformer.Parameters.Statement> dml) {
            this.dml = dml;
        }
    }

    public enum CommitMode {
        PER_STATEMENT,
        PER_TRANSFORM,
        PER_FINISH
    }
}

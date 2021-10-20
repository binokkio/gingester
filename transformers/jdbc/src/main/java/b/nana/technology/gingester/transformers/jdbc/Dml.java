package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.transformers.jdbc.statement.DmlStatement;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class Dml extends JdbcTransformer<Object, Object> {

    private final List<JdbcTransformer.Parameters.Statement> dml;

    private List<DmlStatement> dmlStatements;

    public Dml(Parameters parameters) {
        super(parameters);
        dml = parameters.dml;
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
            }
            getConnection().commit();
        } catch (SQLException e) {
            getConnection().rollback();
            throw e;
        }

        out.accept(context, in);
    }

    public static class Parameters extends JdbcTransformer.Parameters {
        public List<JdbcTransformer.Parameters.Statement> dml;
    }
}

package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;

import java.util.List;

public final class Dml extends JdbcTransformer {

    private final List<Statement> dml;

    private List<PreparedStatement> preparedStatements;

    public Dml(Parameters parameters) {
        super(parameters);
        dml = parameters.dml;
    }

    @Override
    public void open() throws Exception {
        super.open();
        preparedStatements = prepare(dml);
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {
        for (PreparedStatement preparedStatement : preparedStatements) {

        }
    }

    public static class Parameters extends JdbcTransformer.Parameters {
        public List<Statement> dml;
    }
}

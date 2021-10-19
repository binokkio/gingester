package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;

public final class Statement extends JdbcTransformer {

    private final String statement;

    public Statement(Parameters parameters) {
        super(parameters);
        statement = parameters.statement;
    }

    @Override
    public void open() throws Exception {
        super.open();
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {

    }

    public static class Parameters extends JdbcTransformer.Parameters {
        public String statement;
    }
}

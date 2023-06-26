package b.nana.technology.gingester.transformers.jdbc.statement;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.transformers.jdbc.JdbcTransformer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Statement {

    private static final Pattern PARAMETER = Pattern.compile(":([0-9a-zA-Z._-]+)");

    private final PreparedStatement preparedStatement;
    private final List<Parameter> parameters;

    public Statement(Connection connection, String statement, List<JdbcTransformer.Parameters.Statement.Parameter> parameters, boolean returnGeneratedKeys) throws SQLException {

        if (parameters.isEmpty()) {
            parameters = new ArrayList<>();
            Matcher matcher = PARAMETER.matcher(statement);
            while (matcher.find()) {
                JdbcTransformer.Parameters.Statement.Parameter p = new JdbcTransformer.Parameters.Statement.Parameter();
                p.stash = matcher.group(1);
                parameters.add(p);
            }
            statement = matcher.replaceAll("?");
        } else {
            // TODO log deprecation warning
        }

        preparedStatement = returnGeneratedKeys ?
                connection.prepareStatement(statement, java.sql.Statement.RETURN_GENERATED_KEYS) :
                connection.prepareStatement(statement);
        this.parameters = new ArrayList<>();
        for (int i = 0; i < parameters.size(); i++) {
            this.parameters.add(new Parameter(preparedStatement, i + 1, parameters.get(i)));
        }
    }

    protected void updateParameters(Context context) throws SQLException {
        for (Parameter parameter : parameters) {
            parameter.update(context);
        }
    }

    public PreparedStatement getPreparedStatement() {
        return preparedStatement;
    }

    public void close() throws SQLException {
        preparedStatement.close();
    }
}

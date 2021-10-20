package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.core.transformers.Fetch;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.sql.*;
import java.util.*;

public abstract class JdbcTransformer implements Transformer<Object, Object> {

    private final String url;
    private final Properties properties;
    private final List<String> ddl;

    private Connection connection;

    public JdbcTransformer(Parameters parameters) {
        url = parameters.url;
        properties = new Properties();
        properties.putAll(parameters.properties);
        ddl = parameters.ddl;
    }

    @Override
    public void open() throws Exception {

        connection = DriverManager.getConnection(url, properties);

        for (String statement : ddl) {
            connection.prepareStatement(statement).execute();
        }
    }

    protected Connection getConnection() {
        return connection;
    }

    protected List<PreparedStatement> prepare(List<Statement> statements) throws SQLException {
        List<PreparedStatement> preparedStatements = new ArrayList<>();
        for (Statement statement : statements) {
            PreparedStatement pswa = new PreparedStatement();
            pswa.statement = getConnection().prepareStatement(statement.statement);
            pswa.arguments = new ArrayList<>();
            ParameterMetaData parameterMetaData = pswa.statement.getParameterMetaData();
            for (int i = 0; i < statement.arguments.size(); i++) {
                TypedArgument typedArgument = new TypedArgument();
                typedArgument.argument = Fetch.parseStashName(statement.arguments.get(i));
                typedArgument.type = parameterMetaData.getParameterTypeName(i + 1);
                pswa.arguments.add(typedArgument);
            }
            preparedStatements.add(pswa);
        }
        return preparedStatements;
    }

    public static class Parameters {
        public String url;
        public Map<String, Object> properties = Collections.emptyMap();
        public List<String> ddl = Collections.emptyList();
    }

    public static class Statement {
        public String statement;
        public List<String> arguments = Collections.emptyList();

        @JsonCreator
        public Statement() {}

        @JsonCreator
        public Statement(String statement) {
            this.statement = statement;
        }
    }

    protected static class PreparedStatement {
        java.sql.PreparedStatement statement;
        List<TypedArgument> arguments;
    }

    protected static class TypedArgument {
        String[] argument;
        String type;
    }
}

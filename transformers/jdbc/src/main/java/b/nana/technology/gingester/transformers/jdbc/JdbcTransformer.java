package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Phaser;

public abstract class JdbcTransformer<I, O> implements Transformer<I, O> {

    private final String url;
    private final Properties properties;
    private final List<String> ddl;

    private Connection connection;
    private Phaser ddlExecuted;

    public JdbcTransformer(Parameters parameters) {
        url = parameters.url;
        properties = new Properties();
        properties.putAll(parameters.properties);
        ddl = parameters.ddl;
    }

    @Override
    public void setup(SetupControls controls) {
        ddlExecuted = controls.getPhaser("ddl_executed");
        ddlExecuted.register();
    }

    @Override
    public void open() throws Exception {
        connection = DriverManager.getConnection(url, properties);
        connection.setAutoCommit(false);
        try {
            for (String statement : ddl) {
                connection.createStatement().execute(statement);
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
        ddlExecuted.arrive();
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }

    protected Connection getConnection() {
        return connection;
    }

    protected Phaser getDdlExecuted() {
        return ddlExecuted;
    }

    public static class Parameters {

        public String url = "jdbc:sqlite:file::memory:?cache=shared";
        public Map<String, Object> properties = Collections.emptyMap();
        public List<String> ddl = Collections.emptyList();

        public static class Statement {

            public String statement;
            public List<Parameter> parameters = Collections.emptyList();

            @JsonCreator
            public Statement() {}

            @JsonCreator
            public Statement(String statement) {
                this.statement = statement;
            }

            public static class Parameter {

                public String stash;
                public JsonNode instructions;  // can be used to communicate e.g. date formats

                @JsonCreator
                public Parameter() {}

                @JsonCreator
                public Parameter(String stash) {
                    this.stash = stash;
                }
            }
        }
    }
}

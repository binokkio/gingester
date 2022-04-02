package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
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

        if (this instanceof Dml || !ddl.isEmpty()) {
            connection.setAutoCommit(false);
        }

        if (!ddl.isEmpty()) {
            try {
                for (String statement : ddl) {
                    try (Statement s = connection.createStatement()) {
                        s.execute(statement);
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
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

        @JsonDeserialize(using = Statement.Deserializer.class)
        public static class Statement {

            public static class Deserializer extends NormalizingDeserializer<Statement> {
                public Deserializer() {
                    super(Statement.class);
                    rule(JsonNode::isTextual, text -> o("statement", text));
                    rule(json -> json.has("template"), json -> o("statement", json));
                }
            }

            public TemplateParameters statement;
            public List<Parameter> parameters = Collections.emptyList();

            @JsonDeserialize(using = Parameter.Deserializer.class)
            public static class Parameter {

                public static class Deserializer extends NormalizingDeserializer<Parameter> {
                    public Deserializer() {
                        super(Parameter.class);
                        rule(JsonNode::isTextual, text -> o("stash", text));
                    }
                }

                public String stash;
                public JsonNode instructions;  // can be used to communicate e.g. date formats
            }
        }
    }
}

package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.transformer.Transformer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public abstract class JdbcTransformer implements Transformer<Object, Object> {

    private final String url;
    private final Properties properties;
    private final List<String> open;

    private Connection connection;

    public JdbcTransformer(Parameters parameters) {
        url = parameters.url;
        properties = new Properties();
        properties.putAll(parameters.properties);
        open = parameters.open;
    }

    @Override
    public void open() throws Exception {

        connection = DriverManager.getConnection(url, properties);

        for (String sql : open) {
            connection.prepareStatement(sql).execute();
        }
    }

    protected Connection getConnection() {
        return connection;
    }

    public static class Parameters {
        public String url;
        public Map<String, Object> properties = Collections.emptyMap();
        public List<String> open = Collections.emptyList();
    }
}

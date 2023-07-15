package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Passthrough
public final class LoadJson implements Transformer<JsonNode, JsonNode> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final JavaType OBJECT_TYPE = OBJECT_MAPPER.constructType(Object.class);

    private final ContextMap<MixedConnectionsPool<PreparedStatementWith>> contextMap = new ContextMap<>();

    private final Template urlTemplate;
    private final Template tableNameTemplate;
    private final CommitMode commitMode;

    public LoadJson(Parameters parameters) {
        urlTemplate = Context.newTemplate(parameters.url);
        tableNameTemplate = Context.newTemplate(parameters.tableName);
        commitMode = parameters.commitMode;
    }

    @Override
    public void prepare(Context context, Receiver<JsonNode> out) {
        contextMap.put(context, new MixedConnectionsPool<>(
                10,
                0,
                this::onConnectionCreated,
                this::onConnectionMoribund
        ));
    }

    private void onConnectionCreated(ConnectionWith<PreparedStatementWith> connectionWith) {

    }

    private void onConnectionMoribund(ConnectionWith<PreparedStatementWith> connection) throws SQLException {
        if (commitMode == CommitMode.PER_CONNECTION) {
            connection.getConnection().commit();
        }
    }

    @Override
    public void finish(Context context, Receiver<JsonNode> out) throws Exception {
        contextMap.remove(context).close();
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<JsonNode> out) throws Exception {

        contextMap.act(context, state -> {

            ConnectionWith<PreparedStatementWith> connectionWith = state.acquire(urlTemplate.render(context, in));
            try {

                PreparedStatementWith insert = connectionWith.getSingleton();

                if (insert == null) {
                    insert = createTable(context, in, connectionWith.getConnection());
                    connectionWith.setSingleton(insert);
                }

                insert.preparedStatement.clearParameters();

                if (!tryInsert(in, insert)) {
                    insert = alterTable(in, connectionWith);
                    connectionWith.setSingleton(insert);
                    tryInsert(in, insert);
                }

            } finally {
                state.release(connectionWith);
            }
        });
    }

    private PreparedStatementWith createTable(Context context, JsonNode in, Connection connection) throws SQLException {
        String tableName = tableNameTemplate.render(context);
        List<String> keys = new ArrayList<>();
        StringBuilder ddl = new StringBuilder("CREATE TABLE ").append(tableName).append(" (");
        StringBuilder dml = new StringBuilder("INSERT INTO ").append(tableName).append(" VALUES (");

        Iterator<Map.Entry<String, JsonNode>> iterator = in.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();

            if (!value.isNull()) {

                keys.add(key);

                ddl
                        .append(key)
                        .append(' ')
                        .append(getSqlType(key, value))
                        .append(", ");

                dml.append("?, ");
            }
        }

        ddl.setLength(ddl.length() - 2);
        ddl.append(')');
        dml.setLength(dml.length() - 2);
        dml.append(')');

        try (Statement s = connection.createStatement()) {
            s.execute(ddl.toString());
        }

        return new PreparedStatementWith(
                connection.prepareStatement(dml.toString()),
                tableName,
                keys
        );
    }

    private boolean tryInsert(JsonNode in, PreparedStatementWith insert) throws SQLException {
        Iterator<Map.Entry<String, JsonNode>> iterator = in.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            if (!entry.getValue().isNull()) {
                int index = insert.keys.indexOf(entry.getKey()) + 1;
                if (index == 0) return false;
                insert.preparedStatement.setObject(index, OBJECT_MAPPER.convertValue(entry.getValue(), OBJECT_TYPE));
            }
        }
        insert.preparedStatement.execute();
        return true;
    }

    private PreparedStatementWith alterTable(JsonNode in, ConnectionWith<PreparedStatementWith> connectionWith) throws SQLException {

        Connection connection = connectionWith.getConnection();
        PreparedStatementWith preparedStatementWith = connectionWith.getSingleton();
        String tableName = preparedStatementWith.tableName;

        List<String> keys = preparedStatementWith.keys;
        Iterator<Map.Entry<String, JsonNode>> iterator = in.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            if (!value.isNull() && !keys.contains(key)) {
                try (Statement s = connection.createStatement()) {
                    s.execute("ALTER TABLE " + tableName + " ADD COLUMN " + key + " " + getSqlType(key, value));
                }
                keys.add(entry.getKey());
            }
        }

        StringBuilder dml = new StringBuilder("INSERT INTO ").append(tableName).append(" VALUES (");
        for (String ignored : keys) dml.append("?, ");
        dml.setLength(dml.length() - 2);
        dml.append(')');

        return new PreparedStatementWith(
                connection.prepareStatement(dml.toString()),
                tableName,
                keys
        );
    }

    private String getSqlType(String key, JsonNode value) {

        // TODO support per-key overrides in parameters

        if (value.isIntegralNumber()) {
            return "BIGINT";
        } else if (value.isNumber()) {
            return "REAL";
        } else if (value.isTextual()) {
            return "TEXT";
        }

        throw new IllegalArgumentException("No sql type mapping for " + value.getNodeType().name());
    }

    private static class PreparedStatementWith {
        private final PreparedStatement preparedStatement;
        private final String tableName;
        private final List<String> keys;
        private PreparedStatementWith(PreparedStatement preparedStatement, String tableName, List<String> keys) {
            this.preparedStatement = preparedStatement;
            this.tableName = tableName;
            this.keys = keys;
        }
    }

    public static class Parameters {
        public TemplateParameters url = new TemplateParameters("jdbc:sqlite:file::memory:?cache=shared", true);
        public TemplateParameters tableName;
        public CommitMode commitMode = CommitMode.PER_TRANSFORM;
    }

    public enum CommitMode {
        PER_TRANSFORM,
        PER_CONNECTION
    }
}

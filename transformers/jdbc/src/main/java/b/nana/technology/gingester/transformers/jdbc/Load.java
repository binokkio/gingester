package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.controller.ContextMap;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class Load<T, V> implements Transformer<T, T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Load.class);

    private final ContextMap<MixedConnectionsPool<PreparedStatementWith>> contextMap = new ContextMap<>();

    private final Template urlTemplate;
    private final Template tableNameTemplate;
    private final CommitMode commitMode;
    private final char identifierQuote;

    public Load(Parameters parameters) {
        urlTemplate = Context.newTemplate(parameters.url);
        tableNameTemplate = Context.newTemplate(parameters.table);
        commitMode = parameters.commitMode;
        identifierQuote = parameters.identifierQuote;
    }

    @Override
    public void prepare(Context context, Receiver<T> out) {
        contextMap.put(context, new MixedConnectionsPool<>(
                10,
                0,
                this::onConnectionCreated,
                this::onConnectionMoribund
        ));
    }

    private void onConnectionCreated(ConnectionWith<PreparedStatementWith> connectionWith) throws SQLException {
        if (commitMode == CommitMode.PER_CONNECTION) {
            connectionWith.getConnection().setAutoCommit(false);
        }
    }

    private void onConnectionMoribund(ConnectionWith<PreparedStatementWith> connection) throws SQLException {
        if (commitMode == CommitMode.PER_CONNECTION) {
            connection.getConnection().commit();
        }
    }

    @Override
    public void finish(Context context, Receiver<T> out) throws Exception {
        contextMap.remove(context).close();
    }

    @Override
    public void transform(Context context, T in, Receiver<T> out) throws Exception {

        contextMap.act(context, state -> {

            ConnectionWith<PreparedStatementWith> connectionWith = state.acquire(urlTemplate.render(context, in));
            try {

                PreparedStatementWith insert = connectionWith.getSingleton();

                if (insert == null) {
                    insert = initTable(context, in, connectionWith.getConnection());
                    connectionWith.setSingleton(insert);
                }

                if (!tryInsert(in, insert)) {
                    insert = alterTable(in, connectionWith);
                    connectionWith.setSingleton(insert);
                    if (!tryInsert(in, insert)) {
                        throw new IllegalStateException("Data does not match table after table alteration");
                    }
                }

            } finally {
                state.release(connectionWith);
            }
        });

        out.accept(context, in);
    }

    private PreparedStatementWith initTable(Context context, T in, Connection connection) throws SQLException {

        String tableName = tableNameTemplate.render(context);
        StringBuilder dml = new StringBuilder("INSERT INTO ").append(quoteIdentifier(tableName)).append(" (");
        List<String> keys = new ArrayList<>();

        try (ResultSet columns = connection.getMetaData().getColumns(null, null, tableName, null)) {
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                dml.append(quoteIdentifier(columnName)).append(", ");
                keys.add(columnName);
            }
        }

        if (keys.isEmpty()) {

            StringBuilder ddl = new StringBuilder("CREATE TABLE ").append(identifierQuote).append(tableName).append(identifierQuote).append(" (");

            Iterator<Map.Entry<String, V>> iterator = getFields(in);
            while (iterator.hasNext()) {
                Map.Entry<String, V> entry = iterator.next();
                String key = entry.getKey();
                V value = entry.getValue();

                if (!isNull(value)) {

                    keys.add(key);

                    ddl
                            .append(quoteIdentifier(key))
                            .append(' ')
                            .append(getSqlType(key, value))
                            .append(", ");

                    dml
                            .append(quoteIdentifier(key))
                            .append(", ");
                }
            }

            ddl.setLength(ddl.length() - 2);
            ddl.append(')');

            LOGGER.debug("Creating table: " + ddl);

            try (Statement s = connection.createStatement()) {
                s.execute(ddl.toString());
            }
        }

        dml.setLength(dml.length() - 2);
        dml.append(") VALUES (");
        keys.forEach(key -> dml.append("?, "));
        dml.setLength(dml.length() - 2);
        dml.append(')');

        LOGGER.debug("Changing insert: " + dml);

        return new PreparedStatementWith(
                connection.prepareStatement(dml.toString()),
                tableName,
                keys
        );
    }

    private boolean tryInsert(T in, PreparedStatementWith insert) throws SQLException {
        for (int i = 0; i < insert.keys.size(); i++) {
            insert.preparedStatement.setObject(i + 1, null);
        }
        Iterator<Map.Entry<String, V>> iterator = getFields(in);
        while (iterator.hasNext()) {
            Map.Entry<String, V> entry = iterator.next();
            if (!isNull(entry.getValue())) {
                int index = insert.keys.indexOf(entry.getKey()) + 1;
                if (index == 0) return false;
                insert.preparedStatement.setObject(index, convert(entry.getValue()));
            }
        }
        insert.preparedStatement.execute();
        return true;
    }

    private PreparedStatementWith alterTable(T in, ConnectionWith<PreparedStatementWith> connectionWith) throws SQLException {

        Connection connection = connectionWith.getConnection();
        PreparedStatementWith preparedStatementWith = connectionWith.getSingleton();
        String tableName = preparedStatementWith.tableName;

        List<String> keys = preparedStatementWith.keys;
        Iterator<Map.Entry<String, V>> iterator = getFields(in);
        while (iterator.hasNext()) {
            Map.Entry<String, V> entry = iterator.next();
            String key = entry.getKey();
            V value = entry.getValue();
            if (!isNull(value) && !keys.contains(key)) {
                try (Statement s = connection.createStatement()) {
                    String ddl = "ALTER TABLE " + quoteIdentifier(tableName) + " ADD COLUMN " + quoteIdentifier(key) + " " + getSqlType(key, value);
                    LOGGER.debug("Altering table: " + ddl);
                    s.execute(ddl);
                }
                keys.add(entry.getKey());
            }
        }

        StringBuilder dml = new StringBuilder("INSERT INTO ").append(quoteIdentifier(tableName)).append(" (");
        keys.forEach(key -> dml.append(quoteIdentifier(key)).append(", "));
        dml.setLength(dml.length() - 2);
        dml.append(") VALUES (");
        keys.forEach(key -> dml.append("?, "));
        dml.setLength(dml.length() - 2);
        dml.append(')');

        LOGGER.debug("Changing insert: " + dml);

        return new PreparedStatementWith(
                connection.prepareStatement(dml.toString()),
                tableName,
                keys
        );
    }

    private String quoteIdentifier(String identifier) {
        return identifierQuote + identifier + identifierQuote;
    }

    protected abstract Iterator<Map.Entry<String, V>> getFields(T in);
    protected abstract boolean isNull(V value);
    protected abstract String getSqlType(String key, V value);
    protected abstract Object convert(V value);

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
        public TemplateParameters table;
        public CommitMode commitMode = CommitMode.PER_TRANSFORM;
        public char identifierQuote = '"';
    }

    public enum CommitMode {
        PER_TRANSFORM,
        PER_CONNECTION
    }
}

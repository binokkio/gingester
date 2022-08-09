package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class Tables extends JdbcTransformer<Object, String, Void> {

    private final Template catalog;
    private final Template schemaPattern;
    private final Template tableNamePattern;
    private final List<Template> types;

    public Tables(Parameters parameters) {
        super(parameters, true);
        catalog = parameters.catalog != null ? Context.newTemplate(parameters.catalog) : null;
        schemaPattern = parameters.schemaPattern != null ? Context.newTemplate(parameters.schemaPattern) : null;
        tableNamePattern = parameters.tableNamePattern != null ? Context.newTemplate(parameters.tableNamePattern) : null;
        types = parameters.types != null ? parameters.types.stream().map(Context::newTemplate).collect(Collectors.toList()) : null;
    }

    @Override
    public void transform(Context context, Object in, Receiver<String> out) throws Exception {

        ConnectionWith<Void> connection = acquireConnection(context);
        try {

            try (ResultSet resultSet = connection.getConnection().getMetaData().getTables(
                    catalog != null ? catalog.render(context) : null,
                    schemaPattern != null ? schemaPattern.render(context) : null,
                    tableNamePattern != null ? tableNamePattern.render(context) : null,
                    types != null ? types.stream().map(t -> t.render(context)).toArray(String[]::new) : null
            )) {

                while (resultSet.next()) {

                    String catalog = resultSet.getString("TABLE_CAT");
                    String schema = resultSet.getString("TABLE_SCHEM");
                    String table = resultSet.getString("TABLE_NAME");
                    String type = resultSet.getString("TABLE_TYPE");

                    Map<String, Object> stash = new HashMap<>();
                    if (catalog != null) stash.put("catalog", catalog);
                    if (schema != null) stash.put("schema", schema);
                    if (table != null) stash.put("table", table);
                    if (type != null) stash.put("type", type);

                    out.accept(context.stash(stash), table);
                }
            }

        } finally {
            releaseConnection(context, connection);
        }
    }

    public static class Parameters extends JdbcTransformer.Parameters {
        public TemplateParameters catalog;
        public TemplateParameters schemaPattern;
        public TemplateParameters tableNamePattern;
        public List<TemplateParameters> types;
    }
}

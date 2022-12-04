package b.nana.technology.gingester.transformers.jdbc;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateParameters;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class Tables extends JdbcTransformer<Object, String, Void> {

    private final Template catalog;
    private final Template schemaPattern;
    private final Template tableNamePattern;
    private final List<Template> types;
    private final Set<String> excludeCatalogs;
    private final Set<String> excludeSchemas;
    private final Set<String> excludeTables;
    private final Set<String> excludeTypes;

    public Tables(Parameters parameters) {
        super(parameters, true);
        catalog = parameters.catalog != null ? Context.newTemplate(parameters.catalog) : null;
        schemaPattern = parameters.schemaPattern != null ? Context.newTemplate(parameters.schemaPattern) : null;
        tableNamePattern = parameters.tableNamePattern != null ? Context.newTemplate(parameters.tableNamePattern) : null;
        types = parameters.types != null ? parameters.types.stream().map(Context::newTemplate).collect(Collectors.toList()) : null;
        excludeCatalogs = parameters.excludeCatalogs;
        excludeSchemas = parameters.excludeSchemas;
        excludeTables = parameters.excludeTables;
        excludeTypes = parameters.excludeTypes;
    }

    @Override
    public void transform(Context context, Object in, Receiver<String> out) throws Exception {

        ConnectionWith<Void> connection = acquireConnection(context, in);
        try {

            try (ResultSet resultSet = connection.getConnection().getMetaData().getTables(
                    catalog != null ? catalog.render(context, in) : null,
                    schemaPattern != null ? schemaPattern.render(context, in) : null,
                    tableNamePattern != null ? tableNamePattern.render(context, in) : null,
                    types != null ? types.stream().map(t -> t.render(context, in)).toArray(String[]::new) : null
            )) {

                while (resultSet.next()) {

                    Map<String, Object> stash = new HashMap<>();

                    String catalog = resultSet.getString("TABLE_CAT");
                    if (catalog != null) {
                        if (excludeCatalogs.contains(catalog))
                            continue;
                        stash.put("catalog", catalog);
                    }

                    String schema = resultSet.getString("TABLE_SCHEM");
                    if (schema != null) {
                        if (excludeSchemas.contains(schema))
                            continue;
                        stash.put("schema", schema);
                    }

                    String table = resultSet.getString("TABLE_NAME");
                    if (table != null) {
                        System.out.println(table);
                        if (excludeTables.contains(table))
                            continue;
                        stash.put("table", table);
                    }

                    String type = resultSet.getString("TABLE_TYPE");
                    if (type != null) {
                        if (excludeTypes.contains(type))
                            continue;
                        stash.put("type", type);
                    }

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
        public Set<String> excludeCatalogs = Set.of();
        public Set<String> excludeSchemas = Set.of();
        public Set<String> excludeTables = Set.of("sqlite_schema");
        public Set<String> excludeTypes = Set.of();
    }
}

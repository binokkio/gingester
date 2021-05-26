package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.InputStream;
import java.util.List;

public class FromDsvInputStream extends Transformer<InputStream, JsonNode> {

    private final CsvMapper csvMapper;
    private final CsvSchema csvSchema;

    public FromDsvInputStream(Parameters parameters) {
        super(parameters);

        csvMapper = new CsvMapper();
        csvMapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);

        CsvSchema.Builder csvSchemaBuilder = CsvSchema.builder();
        csvSchemaBuilder.setColumnSeparator(parameters.separator);
        csvSchemaBuilder.setQuoteChar(parameters.quote);
        if (parameters.columnNames != null) {
            csvSchema = csvSchemaBuilder.addColumns(parameters.columnNames, CsvSchema.ColumnType.STRING).build();
        } else {
            csvSchema = csvSchemaBuilder.build().withHeader();
        }
    }

    @Override
    protected void transform(Context context, InputStream input) throws Exception {

        long counter = 0;

        MappingIterator<JsonNode> iterator = csvMapper
                .readerFor(JsonNode.class)
                .with(csvSchema)
                .readValues(input);

        while (iterator.hasNext()) {
            JsonNode jsonNode = iterator.next();
            emit(
                    context.extend(this).description(counter++),
                    jsonNode
            );
        }
    }

    public static class Parameters {
        public char separator = ',';
        public char quote = '"';
        public List<String> columnNames;
    }
}

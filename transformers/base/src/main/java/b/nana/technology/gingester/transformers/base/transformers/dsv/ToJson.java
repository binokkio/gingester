package b.nana.technology.gingester.transformers.base.transformers.dsv;

import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class ToJson implements Transformer<InputStream, JsonNode> {

    private final CsvMapper csvMapper;
    private final CsvSchema csvSchema;

    public ToJson(Parameters parameters) {

        csvMapper = new CsvMapper();
        csvMapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);

        CsvSchema.Builder csvSchemaBuilder = CsvSchema.builder();
        csvSchemaBuilder.setColumnSeparator(parameters.delimiter);
        csvSchemaBuilder.setQuoteChar(parameters.quote);
        if (parameters.columnNames != null) {
            csvSchema = csvSchemaBuilder.addColumns(parameters.columnNames, CsvSchema.ColumnType.STRING).build();
        } else {
            csvSchema = csvSchemaBuilder.build().withHeader();
        }
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<JsonNode> out) throws Exception {

        long counter = 0;

        MappingIterator<JsonNode> iterator = csvMapper
                .readerFor(JsonNode.class)
                .with(csvSchema)
                .readValues(in);

        while (iterator.hasNext()) {
            JsonNode jsonNode = iterator.next();
            out.accept(
                    context.stash(Map.of("description", counter++)),
                    jsonNode
            );
        }
    }

    public static class Parameters {
        public char delimiter = ',';
        public char quote = '"';
        public List<String> columnNames;
    }
}
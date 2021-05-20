package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.InputStream;
import java.util.Map;

public class FromCsvInputStream extends Transformer<InputStream, JsonNode> {

    private final CsvMapper csvMapper = new CsvMapper();
    private final CsvSchema csvSchema = csvMapper.schemaWithHeader();

    public FromCsvInputStream(Parameters parameters) {
        super(parameters);
    }

    @Override
    protected void transform(Context context, InputStream input) throws Exception {

        long counter = 0;

        MappingIterator<Map<String, String>> iterator = csvMapper
                .readerForMapOf(String.class)
                .with(csvSchema)
                .readValues(input);

        while (iterator.hasNext()) {
            Map<String, String> row = iterator.next();
            ObjectNode jsonNode = JsonNodeFactory.instance.objectNode();
            row.forEach(jsonNode::put);

            emit(
                    context.extend(this).description(counter++),
                    jsonNode
            );
        }
    }

    public static class Parameters {

    }
}

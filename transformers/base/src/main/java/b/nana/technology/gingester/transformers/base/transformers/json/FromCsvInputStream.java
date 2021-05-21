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

    @Override
    protected void transform(Context context, InputStream input) throws Exception {

        long counter = 0;

        CsvSchema csvSchema = CsvSchema.emptySchema()
                .withHeader();  // TODO add configuration parameters

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
}

package b.nana.technology.gingester.transformers.base.transformers.dsv;

import b.nana.technology.gingester.core.annotations.Description;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.transformers.base.common.string.CharsetTransformer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

@Description("Parse input as delimiter-separated values and output a JSON object for each DSV record")
public final class ToJson extends CharsetTransformer<InputStream, JsonNode> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObjectReader objectReader;
    private final String extras;

    public ToJson(Parameters parameters) {
        super(parameters);

        CsvMapper csvMapper = new CsvMapper();
        csvMapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);

        CsvSchema.Builder csvSchemaBuilder = CsvSchema.builder();
        csvSchemaBuilder.setColumnSeparator(parameters.delimiter);

        if (parameters.quote != null) {
            csvSchemaBuilder.setQuoteChar(parameters.quote);
        } else {
            csvSchemaBuilder.disableQuoteChar();
        }

        if (parameters.escape != null) {
            csvSchemaBuilder.setEscapeChar(parameters.escape);
        } else {
            csvSchemaBuilder.disableEscapeChar();
        }

        extras = parameters.extras;
        if (extras != null) {
            csvSchemaBuilder.setAnyPropertyName(extras);
        }

        CsvSchema csvSchema = parameters.header != null ?
                csvSchemaBuilder.addColumns(parameters.header, CsvSchema.ColumnType.STRING).build() :
                csvSchemaBuilder.build().withHeader();

        objectReader = csvMapper
                .readerFor(MultiMap.class)
                .with(csvSchema);
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<JsonNode> out) throws Exception {
        long counter = 0;
        Reader reader = new InputStreamReader(in, getCharset());
        MappingIterator<MultiMap> iterator = objectReader.readValues(reader);
        while (iterator.hasNext()) {
            MultiMap multiMap = iterator.next();
            ObjectNode result = objectMapper.valueToTree(multiMap);
            if (extras != null && result.path(extras).isValueNode()) {
                ArrayNode arrayNode = objectMapper.createArrayNode();
                arrayNode.add(result.get(extras));
                result.set(extras, arrayNode);
            }
            out.accept(
                    context.stash("description", counter++),
                    result
            );
        }
    }

    public static class Parameters extends CharsetTransformer.Parameters {
        public char delimiter = ',';
        public Character quote = '"';
        public Character escape;
        public List<String> header;
        public String extras = "__extras__";
    }

    private static class MultiMap extends LinkedHashMap<String, Object> {

        private final HashSet<String> upgraded = new HashSet<>();

        @Override
        public Object put(String key, Object value) {
            Object collision = super.put(key, value);
            if (collision != null) {
                if (upgraded.contains(key)) {
                    // noinspection unchecked
                    ((ArrayList<Object>) collision).add(value);
                    super.put(key, collision);
                } else {
                    ArrayList<Object> values = new ArrayList<>();
                    values.add(collision);
                    values.add(value);
                    super.put(key, values);
                    upgraded.add(key);
                }
            }
            return null;
        }
    }
}

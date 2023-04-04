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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            ObjectNode result = iterator.next().objectNode;
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

    private static class MultiMap implements Map<String, String> {

        private final ObjectNode objectNode = JsonNodeFactory.instance.objectNode();

        @Override
        public String put(String key, String value) {
            JsonNode current = objectNode.get(key);
            if (current == null) {
                objectNode.put(key, value);
            } else if (current.isArray()) {
                ((ArrayNode) current).add(value);
            } else {
                ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
                arrayNode.add(current);
                arrayNode.add(value);
                objectNode.set(key, arrayNode);
            }
            return null;
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public boolean containsKey(Object o) {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public boolean containsValue(Object o) {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public String get(Object o) {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public String remove(Object o) {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public void putAll(Map<? extends String, ? extends String> map) {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public Set<String> keySet() {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public Collection<String> values() {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            throw new UnsupportedOperationException("Not implemented");
        }
    }
}

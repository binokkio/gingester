package b.nana.technology.gingester.core.configuration;

import b.nana.technology.gingester.core.Gingester;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GingesterConfiguration {

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(JsonParser.Feature.ALLOW_COMMENTS)
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public static final ObjectReader OBJECT_READER = OBJECT_MAPPER.reader();
    public static final ObjectWriter OBJECT_WRITER = OBJECT_MAPPER.writer(new Printer());

    public static GingesterConfiguration fromJson(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "GingesterConfiguration.fromJson called with null InputStream");
        return OBJECT_MAPPER.readValue(inputStream, GingesterConfiguration.class);
    }

    public Integer report;
    public List<String> excepts = new ArrayList<>();
    public List<TransformerConfiguration> transformers = new ArrayList<>();

    public void append(GingesterConfiguration append) {
        if (append.report != null) report = append.report;
        excepts.addAll(append.excepts);
        transformers.addAll(append.transformers);
    }

    public String toJson() {
        try {
            JsonNode tree = OBJECT_MAPPER.valueToTree(this);
            ArrayNode transformers = (ArrayNode) tree.get("transformers");
            for (int i = 0; i < transformers.size(); i++) {
                if (transformers.get(i).size() == 1) {
                    transformers.set(i, transformers.get(i).get("transformer"));
                }
            }
            return OBJECT_WRITER.writeValueAsString(tree);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Gingester applyTo(Gingester gingester) {
        if (report != null) gingester.report(report);
        gingester.excepts(excepts);
        transformers.forEach(gingester::add);
        return gingester;
    }

    private static class Printer extends DefaultPrettyPrinter {

        Printer() {
            _objectFieldValueSeparatorWithSpaces = ": ";
            _arrayIndenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE;
        }

        @Override
        public DefaultPrettyPrinter createInstance() {
            return new Printer();
        }
    }
}

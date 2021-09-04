package b.nana.technology.gingester.core.configuration;

import b.nana.technology.gingester.core.Gingester;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Configuration {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(JsonParser.Feature.ALLOW_COMMENTS)
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .enable(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    public static final ObjectReader OBJECT_READER = OBJECT_MAPPER.reader();
    public static final ObjectWriter OBJECT_WRITER = OBJECT_MAPPER.writer(new Printer());

    public static Configuration fromJson(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "Configuration.fromJson called with null InputStream");
        return OBJECT_MAPPER.readValue(inputStream, Configuration.class);
    }

    public Boolean report;
    public List<Parameters> transformers = new ArrayList<>();

    public String toJson() {
        try {
            return OBJECT_WRITER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void applyTo(Gingester gingester) {
        transformers.forEach(gingester::add);
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

package b.nana.technology.gingester.transformers.base.transformers.json.extract;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.function.Function;

public class Time extends Transformer<JsonNode, JsonNode> {

    final ZoneId UTC = ZoneId.of("UTC");

    private static final Configuration CONFIGURATION = Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .build();

    private final JsonPath jsonPath;
    private final Function<JsonNode, TemporalAccessor> parser;

    public Time(Parameters parameters) {
        super(parameters);
        jsonPath = JsonPath.compile(parameters.jsonPath);
        parser = getParser(parameters.format);
    }

    private Function<JsonNode, TemporalAccessor> getParser(String format) {
        switch (format) {
            case "MILLIS": return jsonNode -> Instant.ofEpochMilli(jsonNode.asLong()).atZone(UTC);
            case "SECONDS": return jsonNode -> Instant.ofEpochSecond(jsonNode.asLong()).atZone(UTC);
            case "ISO_LOCAL_DATE": return jsonNode -> DateTimeFormatter.ISO_LOCAL_DATE.parse(jsonNode.asText());
            case "ISO_OFFSET_DATE": return jsonNode -> DateTimeFormatter.ISO_OFFSET_DATE.parse(jsonNode.asText());
            case "ISO_DATE": return jsonNode -> DateTimeFormatter.ISO_DATE.parse(jsonNode.asText());
            case "ISO_LOCAL_TIME": return jsonNode -> DateTimeFormatter.ISO_LOCAL_TIME.parse(jsonNode.asText());
            case "ISO_OFFSET_TIME": return jsonNode -> DateTimeFormatter.ISO_OFFSET_TIME.parse(jsonNode.asText());
            case "ISO_TIME": return jsonNode -> DateTimeFormatter.ISO_TIME.parse(jsonNode.asText());
            case "ISO_LOCAL_DATE_TIME": return jsonNode -> DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(jsonNode.asText());
            case "ISO_OFFSET_DATE_TIME": return jsonNode -> DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(jsonNode.asText());
            case "ISO_ZONED_DATE_TIME": return jsonNode -> DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(jsonNode.asText());
            case "ISO_DATE_TIME": return jsonNode -> DateTimeFormatter.ISO_DATE_TIME.parse(jsonNode.asText());
            case "ISO_ORDINAL_DATE": return jsonNode -> DateTimeFormatter.ISO_ORDINAL_DATE.parse(jsonNode.asText());
            case "ISO_WEEK_DATE": return jsonNode -> DateTimeFormatter.ISO_WEEK_DATE.parse(jsonNode.asText());
            case "ISO_INSTANT": return jsonNode -> DateTimeFormatter.ISO_INSTANT.parse(jsonNode.asText());
            case "BASIC_ISO_DATE": return jsonNode -> DateTimeFormatter.BASIC_ISO_DATE.parse(jsonNode.asText());
            case "RFC_1123_DATE_TIME": return jsonNode -> DateTimeFormatter.RFC_1123_DATE_TIME.parse(jsonNode.asText());
            default:
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(format);
                return jsonNode -> dateTimeFormatter.parse(jsonNode.asText());
        }
    }

    @Override
    protected void transform(Context context, JsonNode input) throws Exception {
        DocumentContext documentContext = JsonPath.parse(input, CONFIGURATION);
        JsonNode jsonNode = documentContext.read(jsonPath);
        TemporalAccessor temporalAccessor = parser.apply(jsonNode);
        System.out.println(temporalAccessor);
        // TODO emit, if the JsonNode is necessary later on use stash/fetch
    }

    public static class Parameters {
        public String jsonPath;
        public String format;
        public String zoneId;
    }
}

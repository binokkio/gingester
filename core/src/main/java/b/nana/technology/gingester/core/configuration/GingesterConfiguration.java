package b.nana.technology.gingester.core.configuration;

import b.nana.technology.gingester.core.Gingester;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.ArrayList;
import java.util.List;

public final class GingesterConfiguration {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(JsonParser.Feature.ALLOW_COMMENTS)
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public static final ObjectReader OBJECT_READER = OBJECT_MAPPER.reader();

    public Integer report;
    public List<String> excepts = new ArrayList<>();
    public List<TransformerConfiguration> transformers = new ArrayList<>();

    public void append(GingesterConfiguration append) {
        if (append.report != null) report = append.report;
        excepts.addAll(append.excepts);
        transformers.addAll(append.transformers);
    }

    public Gingester applyTo(Gingester gingester) {
        if (report != null) gingester.report(report);
        gingester.excepts(excepts);
        transformers.forEach(gingester::add);
        return gingester;
    }
}

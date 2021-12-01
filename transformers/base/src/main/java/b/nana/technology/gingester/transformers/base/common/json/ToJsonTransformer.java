package b.nana.technology.gingester.transformers.base.common.json;

import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.util.Collections;
import java.util.List;

public abstract class ToJsonTransformer<I> implements Transformer<I, JsonNode> {

    private final ObjectMapper objectMapper;
    private final ObjectReader objectReader;

    public ToJsonTransformer(Parameters parameters) {
        objectMapper = new ObjectMapper();
        objectMapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        parameters.features.forEach(feature -> objectMapper.enable(feature.mappedFeature()));
        objectReader = objectMapper.reader();
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public ObjectReader getObjectReader() {
        return objectReader;
    }

    public static class Parameters {
        public List<JsonReadFeature> features = Collections.emptyList();
    }
}

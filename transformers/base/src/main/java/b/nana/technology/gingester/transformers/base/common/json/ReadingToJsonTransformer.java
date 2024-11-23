package b.nana.technology.gingester.transformers.base.common.json;

import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.util.Collections;
import java.util.List;

public abstract class ReadingToJsonTransformer<I> implements Transformer<I, JsonNode> {

    private final ObjectReader objectReader;

    protected ReadingToJsonTransformer(Parameters parameters) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        parameters.features.forEach(feature -> objectMapper.enable(feature.mappedFeature()));
        objectReader = objectMapper.reader().forType(JsonNode.class);
    }

    protected ObjectReader getObjectReader() {
        return objectReader;
    }

    public static class Parameters {

        public List<JsonReadFeature> features = Collections.emptyList();

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(List<JsonReadFeature> features) {
            this.features = features;
        }
    }
}

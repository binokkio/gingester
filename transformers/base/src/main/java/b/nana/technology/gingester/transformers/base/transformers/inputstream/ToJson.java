package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class ToJson implements Transformer<InputStream, JsonNode> {

    private final ObjectReader objectReader;

    public ToJson(Parameters parameters) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        parameters.features.forEach(feature -> objectMapper.enable(feature.mappedFeature()));
        objectReader = objectMapper.reader();
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<JsonNode> out) throws Exception {
        out.accept(context, objectReader.readTree(in));
    }

    public static class Parameters {
        public List<JsonReadFeature> features = Collections.emptyList();
    }
}

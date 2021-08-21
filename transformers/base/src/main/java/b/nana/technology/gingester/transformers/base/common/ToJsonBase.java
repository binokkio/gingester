package b.nana.technology.gingester.transformers.base.common;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public abstract class ToJsonBase<I> extends Transformer<I, JsonNode> {

    private final ObjectReader objectReader;

    public ToJsonBase(Parameters parameters) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        parameters.features.forEach(feature -> objectMapper.enable(feature.mappedFeature()));
        objectReader = objectMapper.reader();
    }

    @Override
    protected final void transform(Context context, I input) throws Exception {
        emit(context, objectReader.readTree(toInputStream(input)));
    }

    protected abstract InputStream toInputStream(I input) throws IOException;

    public static class Parameters {
        public List<JsonReadFeature> features = Collections.emptyList();
    }
}

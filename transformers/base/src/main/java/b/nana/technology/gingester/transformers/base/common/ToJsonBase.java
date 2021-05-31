package b.nana.technology.gingester.transformers.base.common;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;
import java.io.InputStream;

public abstract class ToJsonBase<I> extends Transformer<I, JsonNode> {

    private final ObjectReader objectReader;

    public ToJsonBase(Parameters parameters) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        if (parameters.allowSingleQuotes) objectMapper.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
        if (parameters.allowUnquotedFieldNames) objectMapper.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
        objectReader = objectMapper.reader();
    }

    @Override
    protected final void transform(Context context, I input) throws Exception {
        emit(context, objectReader.readTree(toInputStream(input)));
    }

    protected abstract InputStream toInputStream(I input) throws IOException;

    public static class Parameters {
        public boolean allowSingleQuotes;
        public boolean allowUnquotedFieldNames;
    }
}

package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.transformers.base.common.json.IteratingToJsonTransformer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;

@Pure
public final class ToJson extends IteratingToJsonTransformer<String> {

    public ToJson(Parameters parameters) {
        super(parameters);
    }

    @Override
    protected JsonParser getJsonParser(ObjectReader objectReader, String in) throws IOException {
        return objectReader.createParser(in);
    }
}

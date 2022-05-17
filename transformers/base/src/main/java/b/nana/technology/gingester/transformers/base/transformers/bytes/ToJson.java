package b.nana.technology.gingester.transformers.base.transformers.bytes;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.transformers.base.common.json.IteratingToJsonTransformer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;

@Pure
public final class ToJson extends IteratingToJsonTransformer<byte[]> {

    public ToJson(Parameters parameters) {
        super(parameters);
    }

    @Override
    protected JsonParser getJsonParser(ObjectReader objectReader, byte[] in) throws IOException {
        return objectReader.createParser(in);
    }
}

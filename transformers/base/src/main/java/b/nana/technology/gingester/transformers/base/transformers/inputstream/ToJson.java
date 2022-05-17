package b.nana.technology.gingester.transformers.base.transformers.inputstream;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.transformers.base.common.json.IteratingToJsonTransformer;
import b.nana.technology.gingester.transformers.base.common.json.ToJsonTransformer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;
import java.io.InputStream;

@Pure
public final class ToJson extends IteratingToJsonTransformer<InputStream> {

    public ToJson(ToJsonTransformer.Parameters parameters) {
        super(parameters);
    }

    @Override
    protected JsonParser getJsonParser(ObjectReader objectReader, InputStream in) throws IOException {
        return objectReader.createParser(in);
    }
}

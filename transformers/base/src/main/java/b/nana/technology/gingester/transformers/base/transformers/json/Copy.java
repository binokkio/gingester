package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Copy implements Transformer<JsonNode, JsonNode> {

    @Override
    public void transform(Context context, JsonNode in, Receiver<JsonNode> out) throws Exception {
        out.accept(context, in.deepCopy());
    }
}

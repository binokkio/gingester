package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.context.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ToInputStream implements Transformer<JsonNode, InputStream> {

    @Override
    public void transform(Context context, JsonNode in, Receiver<InputStream> out) throws Exception {
        out.accept(context, new ByteArrayInputStream(in.toString().getBytes(StandardCharsets.UTF_8)));
    }
}

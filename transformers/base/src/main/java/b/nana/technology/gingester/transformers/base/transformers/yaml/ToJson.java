package b.nana.technology.gingester.transformers.base.transformers.yaml;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;

public final class ToJson implements Transformer<InputStream, JsonNode> {

    private final ObjectMapper objectMapper;

    public ToJson() {
        objectMapper = new ObjectMapper(new YAMLFactory());
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<JsonNode> out) throws IOException {
        out.accept(context, objectMapper.readTree(in));
    }
}

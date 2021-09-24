package b.nana.technology.gingester.transformers.base.transformers.xml;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.InputStream;

public class ToJson implements Transformer<InputStream, JsonNode> {

    private final XmlMapper xmlMapper = new XmlMapper();

    @Override
    public void transform(Context context, InputStream in, Receiver<JsonNode> out) throws Exception {
        out.accept(context, xmlMapper.readTree(in));
    }
}

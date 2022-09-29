package b.nana.technology.gingester.transformers.base.transformers.xml;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.common.ByteSizeParser;
import com.ctc.wstx.api.WstxInputProperties;
import com.ctc.wstx.stax.WstxInputFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import javax.xml.stream.XMLInputFactory;
import java.io.InputStream;

public final class ToJson implements Transformer<InputStream, JsonNode> {

    private final XmlMapper xmlMapper;

    public ToJson(Parameters parameters) {

        XMLInputFactory xmlInputFactory = new WstxInputFactory();
        xmlInputFactory.setProperty(WstxInputProperties.P_MAX_ATTRIBUTE_SIZE, ByteSizeParser.parse(parameters.maxAttributeSize));

        XmlFactory xmlFactory = XmlFactory.builder()
                .inputFactory(xmlInputFactory)
                .build();

        xmlMapper = new XmlMapper(xmlFactory);
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<JsonNode> out) throws Exception {
        out.accept(context, xmlMapper.readTree(in));
    }

    public static class Parameters {
        public String maxAttributeSize = "10MiB";
    }
}

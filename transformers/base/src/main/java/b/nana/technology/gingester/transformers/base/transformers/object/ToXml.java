package b.nana.technology.gingester.transformers.base.transformers.object;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.util.Collections;
import java.util.List;

public final class ToXml implements Transformer<Object, String> {

    private final XmlMapper xmlMapper;

    public ToXml(Parameters parameters) {
        XmlMapper.Builder builder = XmlMapper.xmlBuilder();
        builder.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        parameters.enable.forEach(builder::enable);
        parameters.disable.forEach(builder::disable);
        xmlMapper = builder.build();
    }

    @Override
    public void transform(Context context, Object in, Receiver<String> out) throws JsonProcessingException {
        out.accept(context, xmlMapper.writeValueAsString(in));
    }

    public static class Parameters {
        public List<SerializationFeature> enable = Collections.emptyList();
        public List<SerializationFeature> disable = Collections.emptyList();
    }
}

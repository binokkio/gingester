package b.nana.technology.gingester.transformers.base.transformers.xml;

import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.InputStream;

public final class ToObject implements Transformer<InputStream, Object> {

    private final XmlMapper xmlMapper = new XmlMapper();
    private final Class<?> type;

    public ToObject(Parameters parameters) throws ClassNotFoundException {
        type = Class.forName(parameters.canonicalName);
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<Object> out) throws Exception {
        out.accept(context, xmlMapper.readValue(in, type));
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order({ "canonicalName" })
    public static class Parameters {
        public String canonicalName;
    }
}

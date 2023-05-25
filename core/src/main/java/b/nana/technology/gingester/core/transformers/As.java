package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.InputStream;

@Names(1)
public final class As implements Transformer<Object, Object> {

    private final Class<?> type;

    public As(Parameters parameters) throws ClassNotFoundException {
        type = getType(parameters.type);
    }

    private Class<?> getType(String type) throws ClassNotFoundException {
        switch (type) {
            case "byte[]": return byte[].class;
            case "InputStream": return InputStream.class;
            case "Json": return JsonNode.class;
            case "String": return String.class;
            default: return Class.forName(type);
        }
    }

    @Override
    public Class<?> getInputType() {
        return type;
    }

    @Override
    public Object getOutputType() {
        return type;
    }

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) {
        out.accept(context, in);
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order("type")
    public static class Parameters {
        public String type;
    }
}

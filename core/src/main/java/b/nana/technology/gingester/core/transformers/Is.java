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
public final class Is implements Transformer<Object, Object> {

    private final Class<?> type;

    public Is(Parameters parameters) throws ClassNotFoundException {
        type = getType(parameters.type);
    }

    public Is(Class<?> type) {
        this.type = type;
    }

    private Class<?> getType(String type) throws ClassNotFoundException {
        return switch (type) {
            case "byte[]" -> byte[].class;
            case "InputStream" -> InputStream.class;
            case "Json" -> JsonNode.class;
            case "String" -> String.class;
            default -> Class.forName(type);
        };
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

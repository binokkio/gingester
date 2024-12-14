package b.nana.technology.gingester.core.transformers;

import b.nana.technology.gingester.core.FlowBuilder;
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

    public As(Class<?> type) {
        this.type = type;
    }

    private Class<?> getType(String type) throws ClassNotFoundException {
        return switch (type) {
            case "Bytes" -> byte[].class;
            case "InputStream" -> InputStream.class;
            case "Json" -> JsonNode.class;
            case "String" -> String.class;
            default -> Class.forName(type);
        };
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

        // maybe we don't need to do anything?
        if (type.isInstance(in)) {
            out.accept(context, in);
            return;
        }

        // TODO add some efficient implementations for common types

        // fallback, build a new flow with a seedValue of `in` and let Gingester build a bridge
        new FlowBuilder()
                .setReportIntervalSeconds(0)
                .seedValue(in)
                .add(new Transformer<>() {

                    @Override
                    public Class<?> getInputType() {
                        return type;
                    }

                    @Override
                    public void transform(Context ignore1, Object in, Receiver<Object> ignore2) {
                        out.accept(context, in);
                    }
                })
                .run();
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order("type")
    public static class Parameters {
        public String type;
    }
}

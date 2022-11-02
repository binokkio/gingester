package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.configuration.NormalizingDeserializer;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Pure
public final class ToString implements Transformer<JsonNode, String> {

    private final ObjectWriter objectWriter = new ObjectMapper()
            .writer(new Printer());

    private final boolean pretty;

    public ToString(Parameters parameters) {
        pretty = parameters.pretty;
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<String> out) throws Exception {
        if (in.isTextual()) {
            out.accept(context, in.textValue());
        } else if (pretty) {
            out.accept(context, objectWriter.writeValueAsString(in));
        } else {
            out.accept(context, in.toString());
        }
    }

    @JsonDeserialize(using = Parameters.Deserializer.class)
    public static class Parameters {
        public static class Deserializer extends NormalizingDeserializer<Parameters> {
            public Deserializer() {
                super(Parameters.class);
                rule(JsonNode::isBoolean, bool -> o("pretty", bool.booleanValue()));
                rule(JsonNode::isTextual, text -> o("pretty", text.textValue().equals("pretty")));
            }
        }

        public boolean pretty;
    }

    private static class Printer extends DefaultPrettyPrinter {

        Printer() {
            _objectFieldValueSeparatorWithSpaces = ": ";
            _arrayIndenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE;
        }

        @Override
        public DefaultPrettyPrinter createInstance() {
            return new Printer();
        }
    }
}

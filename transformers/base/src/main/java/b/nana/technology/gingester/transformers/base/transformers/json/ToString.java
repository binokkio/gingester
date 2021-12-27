package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

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
        if (pretty) {
            out.accept(context, objectWriter.writeValueAsString(in));
        } else {
            out.accept(context, in.toString());
        }
    }

    public static class Parameters {

        public boolean pretty;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(boolean pretty) {
            this.pretty = pretty;
        }
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

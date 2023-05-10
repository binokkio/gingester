package b.nana.technology.gingester.transformers.base.transformers.http;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.configuration.FlagOrderDeserializer;
import b.nana.technology.gingester.core.configuration.Order;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Names(1)
public class ToFormData implements Transformer<JsonNode, String> {

    private final String boundary;

    public ToFormData(Parameters parameters) {
        boundary = parameters.boundary;
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<String> out) throws Exception {

        StringBuilder formData = new StringBuilder();

        in.fields().forEachRemaining(field -> formData
                .append("--")
                .append(boundary)
                .append('\n')
                .append("Content-Disposition: form-data; name=\"")
                .append(field.getKey())
                .append("\"\n\n")
                .append(field.getValue().asText())
                .append('\n'));

        formData
                .append("--")
                .append(boundary)
                .append("--\n\n");

        out.accept(context.stash("boundary", boundary), formData.toString());
    }

    @JsonDeserialize(using = FlagOrderDeserializer.class)
    @Order({ "boundary" })
    public static class Parameters {
        public String boundary = "--------gingester-form-data-boundary";
    }
}

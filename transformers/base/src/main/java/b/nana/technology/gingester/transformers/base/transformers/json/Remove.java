package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;

public final class Remove implements Transformer<JsonNode, JsonNode> {

    private static final Configuration CONFIGURATION = Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .build();

    private final JsonPath jsonPath;

    public Remove(Parameters parameters) {
        jsonPath = JsonPath.compile(parameters.path);
    }

    @Override
    public void transform(Context context, JsonNode in, Receiver<JsonNode> out) {
        DocumentContext documentContext = JsonPath.parse(in, CONFIGURATION).delete(jsonPath);
        out.accept(context, documentContext.json());
    }

    public static class Parameters {

        public String path;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String path) {
            this.path = path;
        }
    }
}

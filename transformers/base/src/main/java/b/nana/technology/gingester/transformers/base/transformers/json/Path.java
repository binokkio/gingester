package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;

public class Path extends Transformer<JsonNode, JsonNode> {

    private static final Configuration CONFIGURATION = Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .build();

    private final String description;
    private final JsonPath jsonPath;

    public Path(Parameters parameters) {
        super(parameters);
        description = parameters.jsonPath;
        jsonPath = JsonPath.compile(parameters.jsonPath);

        if (!jsonPath.isDefinite()) {
            throw new IllegalArgumentException("Json path must be definite");
        }
    }

    @Override
    protected void transform(Context context, JsonNode input) throws Exception {
        DocumentContext documentContext = JsonPath.parse(input, CONFIGURATION);
        JsonNode jsonNode = documentContext.read(jsonPath);
        emit(context.extend(this).description(description), jsonNode);
    }

    public static class Parameters {

        public String jsonPath;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(String jsonPath) {
            this.jsonPath = jsonPath;
        }
    }
}

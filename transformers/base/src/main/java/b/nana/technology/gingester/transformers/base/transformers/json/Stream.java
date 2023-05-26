package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import org.jsfr.json.JsonSurferJackson;
import org.jsfr.json.compiler.JsonPathCompiler;
import org.jsfr.json.path.JsonPath;

import java.io.InputStream;

public final class Stream implements Transformer<InputStream, JsonNode> {

    private final JsonPath jsonPath;

    public Stream(Parameters parameters) {
        jsonPath = JsonPathCompiler.compile(parameters.path);
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<JsonNode> out) {
        JsonSurferJackson.INSTANCE.configBuilder()
                .bind(jsonPath, (o, parsingContext) ->
                        out.accept(
                                context.stash("description", parsingContext.getJsonPath()),
                                (JsonNode) o))
                .buildAndSurf(in);
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

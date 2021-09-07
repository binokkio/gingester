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
import java.util.Iterator;

public class Path implements Transformer<InputStream, JsonNode> {

    private final String descriptionPrefix;
    private final JsonPath jsonPath;

    public Path(Parameters parameters) {
        descriptionPrefix = parameters.path + " :: ";
        jsonPath = JsonPathCompiler.compile(parameters.path);
    }

    @Override
    public void transform(Context context, InputStream in, Receiver<JsonNode> out) {
        long counter = 0;
        Iterator<Object> iterator = JsonSurferJackson.INSTANCE.iterator(in, jsonPath);
        while (iterator.hasNext()) {
            JsonNode jsonNode = (JsonNode) iterator.next();
            out.accept(context.stash("description", descriptionPrefix + counter++), jsonNode);
        }
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

package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;

public class Paths extends Transformer<JsonNode, String> {

    private final boolean emitContainerPaths;

    public Paths(Parameters parameters) {
        super(parameters);
        emitContainerPaths = parameters.emitContainerPaths;
    }

    @Override
    protected void transform(Context context, JsonNode input) throws Exception {
        transform(context, input, "$");
    }

    private void transform(Context context, JsonNode input, String path) {
        if (input.isObject()) {
            if (emitContainerPaths) emit(context, path);
            Iterator<String> keys = input.fieldNames();
            while (keys.hasNext()) {
                String key = keys.next();
                JsonNode value = input.get(key);
                String childPath = path + pathPartFor(key);
                transform(context, value, childPath);
            }
        } else if (input.isArray()) {
            if (emitContainerPaths) emit(context, path);
            for (int i = 0; i < input.size(); i++) {
                JsonNode value = input.get(i);
                String childPath = path + "[]";
                transform(context, value, childPath);
            }
        } else {
            emit(context, path);
        }
    }

    private String pathPartFor(String key) {
        if (key.contains(".")) {
            return "[\"" + key + "\"]";
        } else {
            return '.' + key;
        }
    }

    public static class Parameters {

        public boolean emitContainerPaths = false;

        @JsonCreator
        public Parameters() {}

        @JsonCreator
        public Parameters(boolean emitContainerPaths) {
            this.emitContainerPaths = emitContainerPaths;
        }
    }
}

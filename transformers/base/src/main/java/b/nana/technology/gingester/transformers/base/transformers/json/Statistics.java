package b.nana.technology.gingester.transformers.base.transformers.json;

import b.nana.technology.gingester.core.Context;
import b.nana.technology.gingester.core.Transformer;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

public class Statistics extends Transformer<JsonNode, JsonNode> {

    @Override
    protected void transform(Context context, JsonNode input) throws Exception {

    }

    @Override
    protected void close() throws Exception {
        // TODO emit
    }

    private static class NodeStatistics {

        private final Map<String, NodeStatistics> nested = new HashMap<>();

        private void accept(JsonNode jsonNode) {

        }
    }
}

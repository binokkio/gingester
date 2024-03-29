package b.nana.technology.gingester.transformers.base.transformers.string;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public final class ToJsonNode implements Transformer<String, JsonNode> {

    @Override
    public void transform(Context context, String in, Receiver<JsonNode> out) {
        out.accept(context, JsonNodeFactory.instance.textNode(in));
    }
}

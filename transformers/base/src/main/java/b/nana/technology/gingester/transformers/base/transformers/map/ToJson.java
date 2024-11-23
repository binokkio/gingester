package b.nana.technology.gingester.transformers.base.transformers.map;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.transformers.base.common.json.MappingToJsonTransformer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

@Pure
public final class ToJson extends MappingToJsonTransformer<Map<?, ?>, ObjectNode> {

    @Override
    public void transform(Context context, Map<?, ?> in, Receiver<ObjectNode> out) throws Exception {
        out.accept(context, valueToTree(in));
    }
}

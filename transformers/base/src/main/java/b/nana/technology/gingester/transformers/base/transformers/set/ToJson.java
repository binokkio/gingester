package b.nana.technology.gingester.transformers.base.transformers.set;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.transformers.base.common.json.MappingToJsonTransformer;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Set;

@Pure
public final class ToJson extends MappingToJsonTransformer<Set<?>, ArrayNode> {

    @Override
    public void transform(Context context, Set<?> in, Receiver<ArrayNode> out) throws Exception {
        out.accept(context, valueToTree(in));
    }
}

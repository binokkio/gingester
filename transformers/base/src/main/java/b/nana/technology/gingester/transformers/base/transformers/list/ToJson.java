package b.nana.technology.gingester.transformers.base.transformers.list;

import b.nana.technology.gingester.core.annotations.Pure;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.transformers.base.common.json.MappingToJsonTransformer;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.List;

@Pure
public final class ToJson extends MappingToJsonTransformer<List<?>, ArrayNode> {

    @Override
    public void transform(Context context, List<?> in, Receiver<ArrayNode> out) throws Exception {
        out.accept(context, valueToTree(in));
    }
}

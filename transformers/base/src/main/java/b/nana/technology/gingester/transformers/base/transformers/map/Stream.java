package b.nana.technology.gingester.transformers.base.transformers.map;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.Map;

public final class Stream implements Transformer<Map<?, ?>, Object> {

    @Override
    public void transform(Context context, Map<?, ?> in, Receiver<Object> out) throws Exception {
        for (Map.Entry<?, ?> entry : in.entrySet()) {
            out.accept(context.stash("description", entry.getKey()), entry.getValue());
        }
    }
}

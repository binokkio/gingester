package b.nana.technology.gingester.transformers.base.transformers.list;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.List;

public class Stream implements Transformer<List<?>, Object> {

    @Override
    public void transform(Context context, List<?> in, Receiver<Object> out) throws Exception {
        for (int i = 0; i < in.size(); i++) {
            out.accept(context.stash("description", i), in.get(i));
        }
    }
}

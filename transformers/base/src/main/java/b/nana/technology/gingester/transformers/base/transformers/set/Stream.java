package b.nana.technology.gingester.transformers.base.transformers.set;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.util.Set;

public final class Stream implements Transformer<Set<?>, Object> {

    @Override
    public void transform(Context context, Set<?> in, Receiver<Object> out) throws Exception {
        int i = 0;
        for (Object o : in) {
            out.accept(context.stash("description", i++), o);
        }
    }
}

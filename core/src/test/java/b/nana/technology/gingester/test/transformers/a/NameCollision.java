package b.nana.technology.gingester.test.transformers.a;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

public final class NameCollision implements Transformer<Void, Void> {

    @Override
    public void transform(Context context, Void in, Receiver<Void> out) throws Exception {
        throw new NoSuchMethodException("This transformer only exists to test transformer naming collision resolution");
    }
}

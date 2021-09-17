package b.nana.technology.gingester.test.transformers;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;

public class Throw implements Transformer<Object, Object> {

    @Override
    public void transform(Context context, Object in, Receiver<Object> out) throws Exception {
        throw new Exception("Throw transformer throws");
    }
}
